package cmd

import (
	"fmt"
	"net"

	"github.com/beyond/control-server/internal/service"
	"github.com/beyond/control-server/internal/tray"
	"github.com/beyond/control-server/internal/utils"
)

func Run() {
	utils.Debug("应用启动中...")

	ipAddr := getLocalIP()
	utils.Debug("本机 IP: %s", ipAddr)

	srv := service.New(service.Callbacks{}, ipAddr)

	if err := srv.Start(); err != nil {
		utils.Debug("服务启动失败: %v", err)
		utils.Notify("启动失败", fmt.Sprintf("错误原因: %v", err))
		return
	}

	webURL := "http://localhost:1800"
	wsURL := fmt.Sprintf("ws://%s:1800/ws", ipAddr)

	utils.Debug("Web 界面: %s", webURL)
	utils.Debug("WebSocket: %s", wsURL)

	utils.Notify("控制服务器已启动", fmt.Sprintf("访问地址: %s", webURL))

	tray.Run(webURL, wsURL)

	utils.Debug("应用退出")
}

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
