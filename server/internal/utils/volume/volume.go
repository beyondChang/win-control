//go:build !windows

// Package volume 非Windows平台音量控制占位实现
package volume

import "log"

// VolumeInfo 音量信息
type VolumeInfo struct {
	Level float32 `json:"level"` // 音量等级 0.0~1.0
	Muted bool    `json:"muted"` // 是否静音
}

// GetVolumeInfo 获取音量信息（占位）
func GetVolumeInfo() (*VolumeInfo, error) {
	log.Printf("[VOLUME] GetVolumeInfo() - 非Windows平台暂不支持")
	return &VolumeInfo{Level: 0.5, Muted: false}, nil
}

// SetVolume 设置音量等级（占位）
func SetVolume(level float32) error {
	log.Printf("[VOLUME] SetVolume(%.2f) - 非Windows平台暂不支持", level)
	return nil
}

// SetMute 设置静音状态（占位）
func SetMute(mute bool) error {
	log.Printf("[VOLUME] SetMute(%v) - 非Windows平台暂不支持", mute)
	return nil
}

// ToggleMute 切换静音状态（占位）
func ToggleMute() (bool, error) {
	log.Printf("[VOLUME] ToggleMute() - 非Windows平台暂不支持")
	return false, nil
}

// VolumeUp 增加音量（占位）
func VolumeUp() (float32, error) {
	log.Printf("[VOLUME] VolumeUp() - 非Windows平台暂不支持")
	return 0.5, nil
}

// VolumeDown 减少音量（占位）
func VolumeDown() (float32, error) {
	log.Printf("[VOLUME] VolumeDown() - 非Windows平台暂不支持")
	return 0.5, nil
}
