package test

import (
	"testing"
	"time"

	"github.com/go-vgo/robotgo"
)

// TestMouse 功能测试鼠标移动
func TestMouse(t *testing.T) {
	t.Log("开始鼠标测试，请在 2 秒内停止移动鼠标...")
	time.Sleep(2 * time.Second)

	x, y := robotgo.GetMousePos()
	t.Logf("当前位置: %d, %d", x, y)

	// 测试移动
	t.Log("移动鼠标: 向右下角移动 50 像素")
	robotgo.MoveMouse(x+50, y+50)
	time.Sleep(500 * time.Millisecond)

	newX, newY := robotgo.GetMousePos()
	if newX != x+50 || newY != y+50 {
		t.Errorf("鼠标位置不正确: 期望 (%d, %d), 实际 (%d, %d)", x+50, y+50, newX, newY)
	}

	// 测试点击 (仅记录日志，点击操作较难自动验证)
	t.Log("测试左键点击...")
	robotgo.Click("left")
	time.Sleep(500 * time.Millisecond)
}

// TestKeyboard 功能测试键盘按键
func TestKeyboard(t *testing.T) {
	t.Log("开始键盘测试，请确保焦点在可输入区域...")
	time.Sleep(2 * time.Second)

	// 测试输入字符串
	t.Log("输入测试内容: 'RobotGo Test'")
	robotgo.TypeStr("RobotGo Test")
	time.Sleep(500 * time.Millisecond)

	// 测试按键
	t.Log("按下回车键")
	robotgo.KeyTap("enter")
	time.Sleep(500 * time.Millisecond)
}

// TestCombinationKeys 测试组合键
func TestCombinationKeys(t *testing.T) {
	t.Log("测试组合键")
	time.Sleep(2 * time.Second)

	// robotgo.KeyTap("s", []string{"cmd"}) // Win+S 搜索

	// time.Sleep(2 * time.Second)
	robotgo.KeyTap("tab", []string{"alt"}) // Alt+Tab 切换窗口

	// // 在 Windows 上使用 'command' 模拟 Win 键 (基于之前的测试经验)
	// robotgo.KeyToggle("command", "down")
	// time.Sleep(100 * time.Millisecond)
	// robotgo.KeyTap("r")
	// time.Sleep(100 * time.Millisecond)
	// robotgo.KeyToggle("command", "up")

	t.Log("Win+R 指令已发送")
}

// 测试方向键
func TestDirectionKeys(t *testing.T) {
	t.Log("测试方向键")
	time.Sleep(2 * time.Second)

	robotgo.KeyTap("up")
	time.Sleep(500 * time.Millisecond)
	robotgo.KeyTap("down")
	time.Sleep(500 * time.Millisecond)
	robotgo.KeyTap("left")
	time.Sleep(500 * time.Millisecond)
	robotgo.KeyTap("right")
	time.Sleep(500 * time.Millisecond)
}
