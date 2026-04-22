package cmd

import (
	"fmt"
	"os"
	"server/internal/config"
	"server/internal/server"
	"server/internal/utils/logger"
	"server/internal/utils/tray"
	"server/internal/utils/notification"
	"server/internal/utils/singleinstance"
	"server/web"
)

// Run 应用主入口
func Run() {
	if !singleinstance.Acquire("server") {
		fmt.Println("程序已在运行中")
		os.Exit(1)
	}
	defer singleinstance.Release()

	config.Init()
	logger.Init(config.GetDataDir())
	defer logger.Close()

	server.WebAssets = web.Assets
	apiServer, asm := server.NewServer()
	syncAutoStart(asm)
	logger.Info("服务器已启动，访问地址: http://%s", apiServer.Addr)
	notification.SendWithURL("server 已启动", "访问地址: "+notification.StartURL(), notification.StartURL())

	go func() {
		if err := apiServer.ListenAndServe(); err != nil && err.Error() != "http: Server closed" {
			logger.Error("HTTP服务错误: %v", err)
		}
	}()
	tray.Run(apiServer)
}

// syncAutoStart 根据配置文件中的 auto_start 配置同步实际的自启动注册状态
func syncAutoStart(asm interface{ IsEnabled() bool; Enable() error; Disable() error }) {
	want := config.GetBool("auto_start")
	enabled := asm.IsEnabled()

	if want && !enabled {
		if err := asm.Enable(); err != nil {
			logger.Error("同步开机自启动失败: %v", err)
		}
	} else if !want && enabled {
		if err := asm.Disable(); err != nil {
			logger.Error("同步取消开机自启动失败: %v", err)
		}
	}
}
