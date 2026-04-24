//go:build windows

// Package volume Windows 平台音量控制实现，基于 Core Audio API（CGO）
package volume

/*
#cgo LDFLAGS: -lole32

#include <windows.h>
#include <mmdeviceapi.h>
#include <endpointvolume.h>

// 获取默认音频端点音量接口
static IAudioEndpointVolume* getEndpointVolume() {
    IMMDeviceEnumerator* pEnum = NULL;
    IMMDevice* pDevice = NULL;
    IAudioEndpointVolume* pVolume = NULL;

    HRESULT hr = CoInitializeEx(NULL, COINIT_MULTITHREADED);
    // S_FALSE(0x1) 表示已初始化，RPC_E_CHANGED_MODE(-2147417850) 表示模式冲突，均可继续
    if (FAILED(hr) && hr != S_FALSE && hr != (HRESULT)0x80010106) {
        return NULL;
    }

    const CLSID clsid = {0xBCDE0395, 0xE52F, 0x467C, {0x8E, 0x3D, 0xC4, 0x57, 0x92, 0x91, 0x69, 0x2E}};
    const IID iidEnum = {0xA95664D2, 0x9614, 0x4F35, {0xA7, 0x46, 0xDE, 0x8D, 0xB6, 0x36, 0x17, 0xE6}};
    const IID iidVol  = {0x5CDF2C82, 0x841E, 0x4546, {0x97, 0x22, 0x0C, 0xF7, 0x40, 0x78, 0x22, 0x9A}};

    hr = CoCreateInstance(&clsid, NULL, CLSCTX_ALL, &iidEnum, (void**)&pEnum);
    if (FAILED(hr)) goto done;

    hr = pEnum->lpVtbl->GetDefaultAudioEndpoint(pEnum, eRender, eConsole, &pDevice);
    pEnum->lpVtbl->Release(pEnum);
    if (FAILED(hr)) goto done;

    hr = pDevice->lpVtbl->Activate(pDevice, &iidVol, CLSCTX_ALL, NULL, (void**)&pVolume);
    pDevice->lpVtbl->Release(pDevice);

done:
    return pVolume;
}

// 获取音量等级（0.0~1.0）和静音状态
// 返回 0 成功，非 0 失败
static int cGetVolumeInfo(float* level, int* muted) {
    IAudioEndpointVolume* pVol = getEndpointVolume();
    if (!pVol) return -1;

    HRESULT hr1 = pVol->lpVtbl->GetMasterVolumeLevelScalar(pVol, level);
    BOOL bMuted = FALSE;
    HRESULT hr2 = pVol->lpVtbl->GetMute(pVol, &bMuted);
    pVol->lpVtbl->Release(pVol);
    CoUninitialize();

    *muted = bMuted ? 1 : 0;
    return (FAILED(hr1) || FAILED(hr2)) ? -2 : 0;
}

// 设置音量等级（0.0~1.0）
static int cSetVolume(float level) {
    IAudioEndpointVolume* pVol = getEndpointVolume();
    if (!pVol) return -1;

    HRESULT hr = pVol->lpVtbl->SetMasterVolumeLevelScalar(pVol, level, NULL);
    pVol->lpVtbl->Release(pVol);
    CoUninitialize();

    return FAILED(hr) ? -2 : 0;
}

// 设置静音状态（1=静音，0=取消静音）
static int cSetMute(int mute) {
    IAudioEndpointVolume* pVol = getEndpointVolume();
    if (!pVol) return -1;

    HRESULT hr = pVol->lpVtbl->SetMute(pVol, mute ? TRUE : FALSE, NULL);
    pVol->lpVtbl->Release(pVol);
    CoUninitialize();

    return FAILED(hr) ? -2 : 0;
}
*/
import "C"

import "fmt"

// VolumeInfo 音量信息
type VolumeInfo struct {
	Level float32 `json:"level"` // 音量等级 0.0~1.0
	Muted bool    `json:"muted"` // 是否静音
}

// 音量调节步长
const volumeStep float32 = 0.05

// GetVolumeInfo 获取当前音量信息
func GetVolumeInfo() (*VolumeInfo, error) {
	var level C.float
	var muted C.int

	ret := C.cGetVolumeInfo(&level, &muted)
	if ret != 0 {
		return nil, fmt.Errorf("获取音量信息失败，错误码: %d", int(ret))
	}

	return &VolumeInfo{
		Level: float32(level),
		Muted: muted != 0,
	}, nil
}

// SetVolume 设置音量等级 (0.0 ~ 1.0)
func SetVolume(level float32) error {
	if level < 0 {
		level = 0
	} else if level > 1 {
		level = 1
	}

	ret := C.cSetVolume(C.float(level))
	if ret != 0 {
		return fmt.Errorf("设置音量失败，错误码: %d", int(ret))
	}
	return nil
}

// SetMute 设置静音状态
func SetMute(mute bool) error {
	muteVal := C.int(0)
	if mute {
		muteVal = 1
	}

	ret := C.cSetMute(muteVal)
	if ret != 0 {
		return fmt.Errorf("设置静音失败，错误码: %d", int(ret))
	}
	return nil
}

// ToggleMute 切换静音状态，返回切换后的静音状态
func ToggleMute() (bool, error) {
	info, err := GetVolumeInfo()
	if err != nil {
		return false, err
	}

	newMute := !info.Muted
	if err := SetMute(newMute); err != nil {
		return false, err
	}
	return newMute, nil
}

// VolumeUp 增加音量，返回调整后的音量等级
func VolumeUp() (float32, error) {
	info, err := GetVolumeInfo()
	if err != nil {
		return 0, err
	}

	newLevel := info.Level + volumeStep
	if newLevel > 1.0 {
		newLevel = 1.0
	}

	if err := SetVolume(newLevel); err != nil {
		return 0, err
	}
	return newLevel, nil
}

// VolumeDown 减少音量，返回调整后的音量等级
func VolumeDown() (float32, error) {
	info, err := GetVolumeInfo()
	if err != nil {
		return 0, err
	}

	newLevel := info.Level - volumeStep
	if newLevel < 0 {
		newLevel = 0
	}

	if err := SetVolume(newLevel); err != nil {
		return 0, err
	}
	return newLevel, nil
}
