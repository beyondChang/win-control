package utils

import (
	_ "embed"
	"fmt"
	"log"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"strings"
	"sync"
	"syscall"
)

//go:embed logo.ico
var iconData []byte

var (
	iconOnce     sync.Once
	iconFilePath string
)

// Debug 调试日志
func Debug(format string, args ...interface{}) {
	msg := fmt.Sprintf(format, args...)
	log.Printf("[调试] %s", msg)
}

// Notify 发送系统通知
func Notify(title, message string) {
	Debug("发送通知: %s - %s", title, message)

	switch runtime.GOOS {
	case "windows":
		iconPath := getIconPath()
		cmd := exec.Command("powershell", "-Command",
			fmt.Sprintf(`[Windows.UI.Notifications.ToastNotificationManager, Windows.UI.Notifications, ContentType = WindowsRuntime] | Out-Null; $template = [Windows.UI.Notifications.ToastNotificationManager]::GetTemplateContent([Windows.UI.Notifications.ToastTemplateType]::ToastImageAndText02); $textNodes = $template.GetElementsByTagName('text'); $textNodes.Item(0).AppendChild($template.CreateTextNode('%s')) | Out-Null; $textNodes.Item(1).AppendChild($template.CreateTextNode('%s')) | Out-Null; $imageNodes = $template.GetElementsByTagName('image'); $imageNodes.Item(0).SetAttribute('src', '%s'); $toast = [Windows.UI.Notifications.ToastNotification]::new($template); [Windows.UI.Notifications.ToastNotificationManager]::CreateToastNotifier('ControlServer').Show($toast)`,
				title, message, iconPath))
		hideConsole(cmd)
		cmd.Start()
	case "darwin":
		cmd := exec.Command("osascript", "-e",
			fmt.Sprintf(`display notification "%s" with title "%s"`, message, title))
		cmd.Start()
	default:
		cmd := exec.Command("notify-send", title, message)
		cmd.Start()
	}
}

// getIconPath 获取通知图标路径（从 embed 数据写入临时文件）
func getIconPath() string {
	iconOnce.Do(func() {
		tmpDir := os.TempDir()
		p := filepath.Join(tmpDir, "control-server-logo.ico")
		if err := os.WriteFile(p, iconData, 0644); err != nil {
			Debug("写入图标临时文件失败: %v", err)
			return
		}
		iconFilePath = p
		Debug("图标临时文件: %s", iconFilePath)
	})
	return iconFilePath
}

// CopyToClipboard 将文本复制到系统剪贴板
func CopyToClipboard(text string) error {
	switch runtime.GOOS {
	case "windows":
		cmd := exec.Command("powershell", "-Command",
			fmt.Sprintf(`Set-Clipboard -Value '%s'`, text))
		hideConsole(cmd)
		if err := cmd.Run(); err != nil {
			return fmt.Errorf("复制到剪贴板失败: %v", err)
		}
	case "darwin":
		cmd := exec.Command("pbcopy")
		cmd.Stdin = strings.NewReader(text)
		if err := cmd.Run(); err != nil {
			return fmt.Errorf("复制到剪贴板失败: %v", err)
		}
	default:
		cmd := exec.Command("xclip", "-selection", "clipboard")
		cmd.Stdin = strings.NewReader(text)
		if err := cmd.Run(); err != nil {
			return fmt.Errorf("复制到剪贴板失败: %v", err)
		}
	}
	return nil
}

// OpenURL 在浏览器中打开 URL
func OpenURL(url string) error {
	if url == "" {
		return fmt.Errorf("URL 为空")
	}
	return openWithSystem(url)
}

func openWithSystem(path string) error {
	cmd := buildOpenCommand(path)
	err := cmd.Start()
	if err == nil {
		go func() {
			_ = cmd.Wait()
		}()
	}
	return err
}

func buildOpenCommand(path string) *exec.Cmd {
	switch runtime.GOOS {
	case "darwin":
		return exec.Command("open", path)
	case "windows":
		cmd := exec.Command("cmd", "/c", "start", "", path)
		hideConsole(cmd)
		return cmd
	default:
		return exec.Command("xdg-open", path)
	}
}

// hideConsole 隐藏 Windows 控制台窗口
func hideConsole(cmd *exec.Cmd) {
	if runtime.GOOS == "windows" {
		if cmd.SysProcAttr == nil {
			cmd.SysProcAttr = &syscall.SysProcAttr{}
		}
		cmd.SysProcAttr.HideWindow = true
		// CREATE_NO_WINDOW = 0x08000000
		cmd.SysProcAttr.CreationFlags |= 0x08000000
	}
}
