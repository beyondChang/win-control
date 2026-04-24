//go:build !windows

// Package mouse 非Windows平台鼠标键盘控制占位实现
package mouse

import (
	"log"
)

// MoveMouse 相对移动鼠标（占位）
func MoveMouse(dx, dy int) {
	log.Printf("[MOUSE] MoveMouse(%d, %d) - 非Windows平台暂不支持", dx, dy)
}

// ClickMouse 鼠标点击（占位）
func ClickMouse(button string) {
	log.Printf("[MOUSE] ClickMouse(%s) - 非Windows平台暂不支持", button)
}

// ScrollMouse 鼠标滚轮滚动（占位）
func ScrollMouse(dy int) {
	log.Printf("[MOUSE] ScrollMouse(%d) - 非Windows平台暂不支持", dy)
}

// PressKey 按下按键（占位）
func PressKey(key string) {
	log.Printf("[MOUSE] PressKey(%s) - 非Windows平台暂不支持", key)
}

// OpenSearch 打开系统搜索窗口（占位）
func OpenSearch() {
	log.Printf("[MOUSE] OpenSearch() - 非Windows平台暂不支持")
}

// ShowDesktop 显示桌面（占位）
func ShowDesktop() {
	log.Printf("[MOUSE] ShowDesktop() - 非Windows平台暂不支持")
}
