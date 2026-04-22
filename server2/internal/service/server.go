package service

import (
	"context"
	"encoding/json"
	"fmt"
	"net"
	"net/http"
	"sync"
	"time"

	"github.com/gorilla/websocket"

	"github.com/beyond/control-server/internal/mouse"
	"github.com/beyond/control-server/internal/web"
)

// Command 客户端指令
type Command struct {
	Type string `json:"type"`
	DX   int    `json:"dx,omitempty"`
	DY   int    `json:"dy,omitempty"`
	Key  string `json:"key,omitempty"`
}

// Server 远程控制服务端
type Server struct {
	mu          sync.RWMutex
	running     bool
	connections int
	server      *http.Server
	listener    net.Listener
	cancel      context.CancelFunc
	callbacks   Callbacks
	ipAddr      string
	logCh       chan string
	statusCh    chan *Status
}

// Status 服务状态
type Status struct {
	Running     bool   `json:"running"`
	IP          string `json:"ip"`
	WSAddress   string `json:"wsAddress"`
	Connections int    `json:"connections"`
}

// Callbacks 服务层回调（保留用于日志推送）
type Callbacks struct {
	OnLog              func(msg string)
	OnConnectionChange func(count int)
}

// WebSocket 升级配置
var upgrader = websocket.Upgrader{
	ReadBufferSize:  1024,
	WriteBufferSize: 1024,
	CheckOrigin: func(r *http.Request) bool {
		return true
	},
}

// New 创建 Server 实例
func New(callbacks Callbacks, ipAddr string) *Server {
	return &Server{
		callbacks: callbacks,
		ipAddr:    ipAddr,
		logCh:     make(chan string, 100),
		statusCh:  make(chan *Status, 10),
	}
}

// GetIP 返回本机 IP
func (s *Server) GetIP() string {
	return s.ipAddr
}

// IsRunning 返回服务是否在运行
func (s *Server) IsRunning() bool {
	s.mu.RLock()
	defer s.mu.RUnlock()
	return s.running
}

// GetStatus 获取当前状态
func (s *Server) GetStatus() *Status {
	s.mu.RLock()
	defer s.mu.RUnlock()
	return &Status{
		Running:     s.running,
		IP:          s.ipAddr,
		WSAddress:   fmt.Sprintf("ws://%s:1800/ws", s.ipAddr),
		Connections: s.connections,
	}
}

// Start 启动 HTTP 服务
func (s *Server) Start() error {
	s.mu.Lock()
	if s.running {
		s.mu.Unlock()
		return nil
	}

	mux := http.NewServeMux()

	// API 路由
	mux.HandleFunc("/api/status", s.handleStatus)
	mux.HandleFunc("/api/test", s.handleTest)
	mux.HandleFunc("/api/events", s.handleEvents)

	// WebSocket 路由
	mux.HandleFunc("/ws", s.wsHandler)

	// 前端静态文件
	fileServer := http.FileServer(http.FS(web.Files))
	mux.Handle("/", fileServer)

	ctx, cancel := context.WithCancel(context.Background())

	ln, err := net.Listen("tcp", ":1800")
	if err != nil {
		cancel()
		return fmt.Errorf("监听失败：%w", err)
	}

	s.running = true
	s.listener = ln
	s.cancel = cancel
	s.server = &http.Server{
		Addr:    ":1800",
		Handler: mux,
	}
	s.mu.Unlock()

	s.log(fmt.Sprintf("服务已启动：ws://%s:1800/ws", s.ipAddr))
	s.broadcastStatus()

	go func() {
		err := s.server.Serve(ln)
		if err != nil && err != http.ErrServerClosed {
			s.log(fmt.Sprintf("服务器错误：%v", err))
		}

		select {
		case <-ctx.Done():
		default:
			s.mu.Lock()
			s.running = false
			s.mu.Unlock()
		}
	}()

	return nil
}

// Stop 停止 HTTP 服务
func (s *Server) Stop() {
	s.mu.Lock()
	defer s.mu.Unlock()

	if !s.running {
		return
	}

	s.running = false

	if s.cancel != nil {
		s.cancel()
		s.cancel = nil
	}

	if s.server != nil {
		ctx, cancel := context.WithTimeout(context.Background(), 2*time.Second)
		defer cancel()
		if err := s.server.Shutdown(ctx); err != nil {
			s.log(fmt.Sprintf("服务关闭超时：%v", err))
		}
		s.server = nil
	}

	if s.listener != nil {
		s.listener.Close()
		s.listener = nil
	}

	s.log("服务已停止")
	s.broadcastStatus()
}

// handleStatus 处理状态查询
func (s *Server) handleStatus(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(s.GetStatus())
}

// handleTest 处理测试请求
func (s *Server) handleTest(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	s.log("开始测试键鼠控制...")

	go func() {
		time.Sleep(300 * time.Millisecond)
		mouse.MoveMouse(100, 0)
		s.log("鼠标右移 100px")

		time.Sleep(300 * time.Millisecond)
		mouse.MoveMouse(-100, 0)
		s.log("鼠标左移 100px")

		time.Sleep(300 * time.Millisecond)
		mouse.ClickMouse("left")
		s.log("左键点击")

		time.Sleep(300 * time.Millisecond)
		s.log("测试完成")
	}()

	json.NewEncoder(w).Encode(map[string]string{"status": "testing"})
}

// handleEvents 处理 SSE 事件流
func (s *Server) handleEvents(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "text/event-stream")
	w.Header().Set("Cache-Control", "no-cache")
	w.Header().Set("Connection", "keep-alive")
	w.Header().Set("Access-Control-Allow-Origin", "*")

	flusher, ok := w.(http.Flusher)
	if !ok {
		http.Error(w, "Streaming unsupported", http.StatusInternalServerError)
		return
	}

	// 发送初始状态
	status := s.GetStatus()
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
			case status := <-s.statusCh:
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
			case msg := <-s.logCh:
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

// log 记录日志
func (s *Server) log(msg string) {
	timestamp := time.Now().Format("15:04:05")
	fullMsg := fmt.Sprintf("[%s] %s", timestamp, msg)

	if s.callbacks.OnLog != nil {
		s.callbacks.OnLog(fullMsg)
	}

	// 发送到 SSE
	select {
	case s.logCh <- fullMsg:
	default:
	}
}

// broadcastStatus 广播状态变化
func (s *Server) broadcastStatus() {
	select {
	case s.statusCh <- s.GetStatus():
	default:
	}
}

// wsHandler 处理 WebSocket 连接
func (s *Server) wsHandler(w http.ResponseWriter, r *http.Request) {
	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		s.log(fmt.Sprintf("WebSocket 升级失败：%v", err))
		return
	}
	defer conn.Close()

	s.mu.Lock()
	s.connections++
	count := s.connections
	s.mu.Unlock()

	s.broadcastStatus()
	s.log(fmt.Sprintf("新客户端连接 (共%d个) - IP: %s", count, r.RemoteAddr))
	s.log(fmt.Sprintf("客户端 %s 进入读取循环", r.RemoteAddr))

	conn.SetReadLimit(4096)
	conn.SetReadDeadline(time.Now().Add(60 * time.Second))
	conn.SetPongHandler(func(string) error {
		conn.SetReadDeadline(time.Now().Add(60 * time.Second))
		return nil
	})

	for {
		s.mu.RLock()
		running := s.running
		s.mu.RUnlock()

		if !running {
			s.log(fmt.Sprintf("服务器停止，客户端 %s 断开连接", r.RemoteAddr))
			break
		}

		conn.SetReadDeadline(time.Now().Add(60 * time.Second))
		_, msg, err := conn.ReadMessage()
		if err != nil {
			if netErr, ok := err.(net.Error); ok && netErr.Timeout() {
				continue
			}

			if websocket.IsUnexpectedCloseError(err, websocket.CloseGoingAway, websocket.CloseAbnormalClosure) {
				s.log(fmt.Sprintf("客户端 %s 连接异常关闭：%v", r.RemoteAddr, err))
			} else {
				s.log(fmt.Sprintf("客户端 %s 连接关闭", r.RemoteAddr))
			}
			break
		}

		var cmd Command
		if err := json.Unmarshal(msg, &cmd); err != nil {
			s.log(fmt.Sprintf("客户端 %s 解析命令失败：%v", r.RemoteAddr, err))
			continue
		}

		s.executeCommand(cmd)
	}

	s.mu.Lock()
	s.connections--
	count = s.connections
	s.mu.Unlock()

	s.broadcastStatus()
	s.log(fmt.Sprintf("客户端 %s 断开连接 (共%d个)", r.RemoteAddr, count))
}

// executeCommand 执行客户端指令
func (s *Server) executeCommand(cmd Command) {
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
	default:
		logMsg = fmt.Sprintf("收到未知指令: %s", cmd.Type)
	}

	if logMsg != "" {
		s.log(logMsg)
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
	}
}

// getLocalIP 获取本机局域网 IP
func getLocalIP() string {
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
