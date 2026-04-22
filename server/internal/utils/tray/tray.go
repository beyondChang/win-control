package tray

import (
	"context"
	"fmt"
	"net/http"
	"os/exec"
	"os/signal"
	"runtime"
	"syscall"
	"time"

	"fyne.io/systray"

	"server/internal/config"
	"server/internal/utils/logger"
)

// Run 启动系统托盘，阻塞运行直到用户选择退出
func Run(apiServer *http.Server) {
	systray.Run(func() { onReady(apiServer) }, onExit)
}

// onReady 托盘就绪回调：设置图标、菜单项和点击事件处理
func onReady(apiServer *http.Server) {
	systray.SetIcon(IconData())
	systray.SetTitle("server")
	systray.SetTooltip("server")

	// 添加菜单项
	mOpen := systray.AddMenuItem("打开主页面", "在浏览器中打开主页面")
	systray.AddSeparator()
	mQuit := systray.AddMenuItem("退出", "退出程序")

	// 监听菜单点击事件
	go func() {
		for {
			select {
			case <-mOpen.ClickedCh:
				openBrowser(fmt.Sprintf("http://localhost:%d", config.GetPort()))
			case <-mQuit.ClickedCh:
				shutdown(apiServer)
				systray.Quit()
			}
		}
	}()

	// 监听系统中断信号（Ctrl+C）
	go func() {
		ctx, stop := signal.NotifyContext(context.Background(), syscall.SIGINT, syscall.SIGTERM)
		defer stop()
		<-ctx.Done()
		logger.Info("收到退出信号")
		shutdown(apiServer)
		systray.Quit()
	}()
}

// onExit 托盘退出回调
func onExit() {
	logger.Info("托盘已退出")
}

// shutdown 优雅关闭 HTTP 服务器，最多等待 5 秒
func shutdown(apiServer *http.Server) {
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	if err := apiServer.Shutdown(ctx); err != nil {
		logger.Error("服务器强制关闭: %v", err)
	}
	logger.Info("服务器已停止")
}

// openBrowser 使用系统默认浏览器打开指定 URL
func openBrowser(url string) {
	var cmd *exec.Cmd
	switch runtime.GOOS {
	case "windows":
		cmd = exec.Command("rundll32", "url.dll,FileProtocolHandler", url)
	case "darwin":
		cmd = exec.Command("open", url)
	default:
		cmd = exec.Command("xdg-open", url)
	}
	if err := cmd.Start(); err != nil {
		logger.Error("打开浏览器失败: %v", err)
	}
}
