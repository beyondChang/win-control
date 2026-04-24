// 远程控制相关 HTTP 处理器
package handler

import (
	"encoding/json"
	"fmt"
	"net"
	"net/http"
	"sync"
	"time"

	"server/internal/utils/logger"
	"server/internal/utils/mouse"
	"server/internal/utils/volume"

	"github.com/coder/websocket"
)

// ControlHandler 远程控制处理器
type ControlHandler struct {
	mu          sync.RWMutex
	connections int
	running     bool
	logCh       chan string
	statusCh    chan *ControlStatus
}

// ControlStatus 控制服务状态
type ControlStatus struct {
	Running     bool   `json:"running"`
	IP          string `json:"ip"`
	WSAddress   string `json:"wsAddress"`
	Connections int    `json:"connections"`
}

// Command 客户端指令
type Command struct {
	Type  string  `json:"type"`
	DX    int     `json:"dx,omitempty"`
	DY    int     `json:"dy,omitempty"`
	Key   string  `json:"key,omitempty"`
	Level float32 `json:"level,omitempty"` // 音量等级，用于 volumeSet 指令
	Mute  *bool   `json:"mute,omitempty"`  // 静音状态，用于 volumeMute 指令
}

// 全局控制处理器实例
var controlHandler *ControlHandler

// GetControlHandler 获取控制处理器单例
func GetControlHandler() *ControlHandler {
	if controlHandler == nil {
		controlHandler = &ControlHandler{
			logCh:    make(chan string, 100),
			statusCh: make(chan *ControlStatus, 10),
		}
	}
	return controlHandler
}

// Start 标记服务启动
func (h *ControlHandler) Start() {
	h.mu.Lock()
	h.running = true
	h.mu.Unlock()
	h.log("远程控制服务已启动")
	h.broadcastStatus()
}

// GetIP 获取本机局域网 IP
func (h *ControlHandler) GetIP() string {
	addrs, err := net.InterfaceAddrs()
	if err != nil {
		return "127.0.0.1"
	}
	for _, addr := range addrs {
		if ipNet, ok := addr.(*net.IPNet); ok && !ipNet.IP.IsLoopback() {
			if ipNet.IP.To4() != nil {
				return ipNet.IP.String()
			}
		}
	}
	return "127.0.0.1"
}

// GetStatus 获取当前状态
func (h *ControlHandler) GetStatus() *ControlStatus {
	h.mu.RLock()
	defer h.mu.RUnlock()
	ip := h.GetIP()
	return &ControlStatus{
		Running:     h.running,
		IP:          ip,
		WSAddress:   fmt.Sprintf("ws://%s:1800/ws/control", ip),
		Connections: h.connections,
	}
}

// HandleStatus 处理状态查询 API
func (h *ControlHandler) HandleStatus(w http.ResponseWriter, r *http.Request) {
	writeJSON(w, http.StatusOK, h.GetStatus())
}

// HandleTest 处理测试请求
func (h *ControlHandler) HandleTest(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		writeError(w, http.StatusMethodNotAllowed, "方法不允许")
		return
	}

	h.log("开始测试键鼠控制...")

	go func() {
		time.Sleep(300 * time.Millisecond)
		mouse.MoveMouse(100, 0)
		h.log("鼠标右移 100px")

		time.Sleep(300 * time.Millisecond)
		mouse.MoveMouse(-100, 0)
		h.log("鼠标左移 100px")

		time.Sleep(300 * time.Millisecond)
		mouse.ClickMouse("left")
		h.log("左键点击")

		time.Sleep(300 * time.Millisecond)
		h.log("测试完成")
	}()

	writeJSON(w, http.StatusOK, map[string]string{"status": "testing"})
}

// HandleEvents 处理 SSE 事件流
func (h *ControlHandler) HandleEvents(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "text/event-stream")
	w.Header().Set("Cache-Control", "no-cache")
	w.Header().Set("Connection", "keep-alive")
	w.Header().Set("Access-Control-Allow-Origin", "*")

	flusher, ok := w.(http.Flusher)
	if !ok {
		http.Error(w, "不支持流式传输", http.StatusInternalServerError)
		return
	}

	// 发送初始状态
	status := h.GetStatus()
	data, _ := json.Marshal(map[string]interface{}{"type": "status", "data": status})
	fmt.Fprintf(w, "data: %s\n\n", data)
	flusher.Flush()

	// 监听状态变化
	statusDone := make(chan bool)
	go func() {
		for {
			select {
			case <-statusDone:
				return
			case status := <-h.statusCh:
				data, _ := json.Marshal(map[string]interface{}{"type": "status", "data": status})
				fmt.Fprintf(w, "data: %s\n\n", data)
				flusher.Flush()
			}
		}
	}()

	// 监听日志
	logDone := make(chan bool)
	go func() {
		for {
			select {
			case <-logDone:
				return
			case msg := <-h.logCh:
				data, _ := json.Marshal(map[string]interface{}{"type": "log", "data": msg})
				fmt.Fprintf(w, "data: %s\n\n", data)
				flusher.Flush()
			}
		}
	}()

	<-r.Context().Done()
	close(statusDone)
	close(logDone)
}

// HandleControlWS 处理远程控制 WebSocket 连接
func (h *ControlHandler) HandleControlWS(w http.ResponseWriter, r *http.Request) {
	socket, err := websocket.Accept(w, r, &websocket.AcceptOptions{
		OriginPatterns: []string{"*"},
	})
	if err != nil {
		logger.Error("WebSocket 升级失败: %v", err)
		return
	}
	defer socket.Close(1000, "连接关闭")

	h.mu.Lock()
	h.connections++
	count := h.connections
	h.mu.Unlock()

	h.broadcastStatus()
	h.log(fmt.Sprintf("新客户端连接 (共%d个) - IP: %s", count, r.RemoteAddr))

	ctx := r.Context()

	for {
		h.mu.RLock()
		running := h.running
		h.mu.RUnlock()

		if !running {
			h.log(fmt.Sprintf("服务器停止，客户端 %s 断开连接", r.RemoteAddr))
			break
		}

		_, msg, err := socket.Read(ctx)
		if err != nil {
			if netErr, ok := err.(net.Error); ok && netErr.Timeout() {
				continue
			}
			h.log(fmt.Sprintf("客户端 %s 连接关闭", r.RemoteAddr))
			break
		}

		var cmd Command
		if err := json.Unmarshal(msg, &cmd); err != nil {
			h.log(fmt.Sprintf("客户端 %s 解析命令失败: %v", r.RemoteAddr, err))
			continue
		}

		h.executeCommand(cmd)
	}

	h.mu.Lock()
	h.connections--
	count = h.connections
	h.mu.Unlock()

	h.broadcastStatus()
	h.log(fmt.Sprintf("客户端 %s 断开连接 (共%d个)", r.RemoteAddr, count))
}

// executeCommand 执行客户端指令
func (h *ControlHandler) executeCommand(cmd Command) {
	var logMsg string
	switch cmd.Type {
	case "click":
		logMsg = "收到点击指令: 左键"
	case "rightClick":
		logMsg = "收到点击指令: 右键"
	case "key":
		logMsg = fmt.Sprintf("收到按键指令: %s", cmd.Key)
	case "move", "scroll":
		// 移动和滚动指令过于频繁，不记录日志
		break
	case "volumeSet":
		logMsg = fmt.Sprintf("收到音量设置指令: %.0f%%", cmd.Level*100)
	case "volumeUp":
		logMsg = "收到音量增加指令"
	case "volumeDown":
		logMsg = "收到音量减少指令"
	case "volumeMute":
		logMsg = "收到静音切换指令"
	default:
		logMsg = fmt.Sprintf("收到未知指令: %s", cmd.Type)
	}

	if logMsg != "" {
		h.log(logMsg)
	}

	switch cmd.Type {
	case "move":
		mouse.MoveMouse(cmd.DX, cmd.DY)
	case "click":
		mouse.ClickMouse("left")
	case "rightClick":
		mouse.ClickMouse("right")
	case "scroll":
		mouse.ScrollMouse(cmd.DY)
	case "key":
		if cmd.Key == "search" {
			mouse.OpenSearch()
		} else {
			mouse.PressKey(cmd.Key)
		}
	case "volumeSet":
		// 设置音量等级
		if err := volume.SetVolume(cmd.Level); err != nil {
			h.log(fmt.Sprintf("设置音量失败: %v", err))
		}
	case "volumeUp":
		// 增加音量
		if newLevel, err := volume.VolumeUp(); err != nil {
			h.log(fmt.Sprintf("增加音量失败: %v", err))
		} else {
			h.log(fmt.Sprintf("音量已调整为 %.0f%%", newLevel*100))
		}
	case "volumeDown":
		// 减少音量
		if newLevel, err := volume.VolumeDown(); err != nil {
			h.log(fmt.Sprintf("减少音量失败: %v", err))
		} else {
			h.log(fmt.Sprintf("音量已调整为 %.0f%%", newLevel*100))
		}
	case "volumeMute":
		// 切换静音状态
		if cmd.Mute != nil {
			// 指定静音状态
			if err := volume.SetMute(*cmd.Mute); err != nil {
				h.log(fmt.Sprintf("设置静音失败: %v", err))
			}
		} else {
			// 切换静音
			if muted, err := volume.ToggleMute(); err != nil {
				h.log(fmt.Sprintf("切换静音失败: %v", err))
			} else {
				if muted {
					h.log("已静音")
				} else {
					h.log("已取消静音")
				}
			}
		}
	}
}

// VolumeRequest 音量控制请求体
type VolumeRequest struct {
	Action string  `json:"action"`           // 操作类型: set/up/down/mute/unmute/toggle
	Level  float32 `json:"level,omitempty"`  // 音量等级 (0.0~1.0)，action=set 时必填
}

// HandleVolume 获取音量信息或控制音量
func (h *ControlHandler) HandleVolume(w http.ResponseWriter, r *http.Request) {
	switch r.Method {
	case http.MethodGet:
		// 获取音量信息
		info, err := volume.GetVolumeInfo()
		if err != nil {
			writeError(w, http.StatusInternalServerError, fmt.Sprintf("获取音量信息失败: %v", err))
			return
		}
		writeJSON(w, http.StatusOK, info)

	case http.MethodPost:
		// 控制音量
		var req VolumeRequest
		if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
			writeError(w, http.StatusBadRequest, "无效的请求数据")
			return
		}

		var result *volume.VolumeInfo
		var logMsg string

		switch req.Action {
		case "set":
			// 设置音量等级
			if req.Level < 0 || req.Level > 1 {
				writeError(w, http.StatusBadRequest, "音量等级必须在 0.0~1.0 之间")
				return
			}
			if err := volume.SetVolume(req.Level); err != nil {
				writeError(w, http.StatusInternalServerError, fmt.Sprintf("设置音量失败: %v", err))
				return
			}
			logMsg = fmt.Sprintf("音量已设置为 %.0f%%", req.Level*100)
		case "up":
			// 增加音量
			newLevel, err := volume.VolumeUp()
			if err != nil {
				writeError(w, http.StatusInternalServerError, fmt.Sprintf("增加音量失败: %v", err))
				return
			}
			logMsg = fmt.Sprintf("音量已调整为 %.0f%%", newLevel*100)
		case "down":
			// 减少音量
			newLevel, err := volume.VolumeDown()
			if err != nil {
				writeError(w, http.StatusInternalServerError, fmt.Sprintf("减少音量失败: %v", err))
				return
			}
			logMsg = fmt.Sprintf("音量已调整为 %.0f%%", newLevel*100)
		case "mute":
			// 静音
			if err := volume.SetMute(true); err != nil {
				writeError(w, http.StatusInternalServerError, fmt.Sprintf("静音失败: %v", err))
				return
			}
			logMsg = "已静音"
		case "unmute":
			// 取消静音
			if err := volume.SetMute(false); err != nil {
				writeError(w, http.StatusInternalServerError, fmt.Sprintf("取消静音失败: %v", err))
				return
			}
			logMsg = "已取消静音"
		case "toggle":
			// 切换静音
			muted, err := volume.ToggleMute()
			if err != nil {
				writeError(w, http.StatusInternalServerError, fmt.Sprintf("切换静音失败: %v", err))
				return
			}
			if muted {
				logMsg = "已静音"
			} else {
				logMsg = "已取消静音"
			}
		default:
			writeError(w, http.StatusBadRequest, fmt.Sprintf("未知操作: %s，支持: set/up/down/mute/unmute/toggle", req.Action))
			return
		}

		h.log(logMsg)

		// 返回最新的音量信息
		info, err := volume.GetVolumeInfo()
		if err != nil {
			writeError(w, http.StatusInternalServerError, fmt.Sprintf("获取音量信息失败: %v", err))
			return
		}
		result = info
		writeJSON(w, http.StatusOK, result)

	default:
		writeError(w, http.StatusMethodNotAllowed, "方法不允许")
	}
}

// log 记录日志
func (h *ControlHandler) log(msg string) {
	timestamp := time.Now().Format("15:04:05")
	fullMsg := fmt.Sprintf("[%s] %s", timestamp, msg)

	logger.Info("[Control] %s", msg)

	// 发送到 SSE
	select {
	case h.logCh <- fullMsg:
	default:
	}
}

// broadcastStatus 广播状态变化
func (h *ControlHandler) broadcastStatus() {
	select {
	case h.statusCh <- h.GetStatus():
	default:
	}
}
