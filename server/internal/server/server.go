package server

import (
	"fmt"
	"net/http"
	"time"

	"server/internal/config"
	"server/internal/database"
	"server/internal/handler"
	"server/internal/utils/autostart"
)

// Server HTTP 服务器结构体
type Server struct {
	port    int
	handler *handler.Handler
	autostartManager autostart.Manager
}

// NewServer 创建并配置 HTTP 服务器
func NewServer() (*http.Server, autostart.Manager) {
	port := config.GetPort()
	db := database.New()
	// 根据 local_only 配置决定绑定地址
	addr := fmt.Sprintf(":%d", port)
	if config.GetBool("local_only") {
		addr = fmt.Sprintf("127.0.0.1:%d", port)
	}
	asm := autostart.New("server")

	s := &Server{
		port: port,
		handler:          handler.New(db, asm),
		autostartManager: asm,
	}

	server := &http.Server{
		Addr:         addr,
		Handler:      s.RegisterRoutes(),
		IdleTimeout:  time.Minute,
		ReadTimeout:  10 * time.Second,
		WriteTimeout: 30 * time.Second,
	}

	return server, asm
}
