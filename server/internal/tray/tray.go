package tray

import (
	"fmt"

	"github.com/beyond/control-server/internal/assets"
	"github.com/beyond/control-server/internal/utils"
	"github.com/getlantern/systray"
)

var webURL string
var wsURL string

// Run 启动系统托盘循环
func Run(url, ws string) {
	utils.Debug("托盘启动，地址: %s", url)
	webURL = url
	wsURL = ws
	utils.Debug("调用 systray.Run()")
	systray.Run(onReady, onExit)
	utils.Debug("systray.Run() 已返回")
}

func onReady() {
	utils.Debug("托盘初始化开始")
	defer func() {
		if r := recover(); r != nil {
			utils.Debug("托盘初始化异常: %v", r)
		}
	}()

	utils.Debug("设置托盘图标")
	systray.SetIcon(assets.Logo)

	systray.SetTooltip("远程键鼠控制服务器")

	utils.Debug("添加托盘菜单项")
	mShow := systray.AddMenuItem("显示主界面", "打开控制服务器 Web 界面")
	mCopy := systray.AddMenuItem("复制连接地址", "复制 WebSocket 连接地址")
	systray.AddSeparator()
	mQuit := systray.AddMenuItem("退出", "关闭控制服务器")

	go func() {
		utils.Debug("托盘菜单事件循环已启动")
		for {
			select {
			case <-mShow.ClickedCh:
				utils.Debug("点击菜单: 显示主界面")
				openWeb()
			case <-mCopy.ClickedCh:
				utils.Debug("点击菜单: 复制连接地址")
				copyToClipboard()
			case <-mQuit.ClickedCh:
				utils.Debug("点击菜单: 退出")
				systray.Quit()
			}
		}
	}()
	utils.Debug("托盘初始化完成")
}

func onExit() {
	utils.Debug("通过托盘退出应用")
}

func openWeb() {
	if webURL != "" {
		if err := utils.OpenURL(webURL); err != nil {
			utils.Debug("打开浏览器失败: %v", err)
		}
	}
}

func copyToClipboard() {
	if wsURL == "" {
		utils.Debug("无连接地址可复制")
		return
	}
	if err := utils.CopyToClipboard(wsURL); err != nil {
		utils.Debug("复制地址失败: %v", err)
		utils.Notify("复制失败", err.Error())
		return
	}
	utils.Debug("已复制连接地址: %s", wsURL)
	utils.Notify("复制成功", fmt.Sprintf("已复制: %s", wsURL))
}
