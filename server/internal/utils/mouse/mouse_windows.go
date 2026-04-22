//go:build windows

// Package mouse Windows 平台鼠标键盘控制实现
package mouse

import (
	"github.com/go-vgo/robotgo"
)

// MoveMouse 相对移动鼠标
func MoveMouse(dx, dy int) {
	x, y := robotgo.GetMousePos()
	robotgo.MoveMouse(x+dx, y+dy)
}

// ClickMouse 鼠标点击
func ClickMouse(button string) {
	robotgo.Click(button)
}

// ScrollMouse 鼠标滚轮滚动
func ScrollMouse(dy int) {
	robotgo.Scroll(0, dy)
}

// PressKey 按下按键
func PressKey(key string) {
	robotgo.KeyTap(key)
}

// OpenSearch 打开系统搜索窗口 (Win+S)
func OpenSearch() {
	robotgo.KeyTap("s", []string{"cmd"})
}
