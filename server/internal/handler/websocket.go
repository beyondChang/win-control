package handler

import (
	"fmt"
	"net/http"
	"time"
	"server/internal/utils/logger"

	"github.com/coder/websocket"
)

// WebSocket 处理 WebSocket 连接，接收消息并回复
func (h *Handler) WebSocket(w http.ResponseWriter, r *http.Request) {
	socket, err := websocket.Accept(w, r, nil)
	if err != nil {
		logger.Error("WebSocket连接失败: %v", err)
		w.WriteHeader(http.StatusInternalServerError)
		return
	}
	defer socket.Close(websocket.StatusGoingAway, "服务器关闭连接")
	logger.Info("WebSocket新连接: %s", r.RemoteAddr)

	ctx := r.Context()

	// 循环读取消息并回复
	for {
		msgType, data, err := socket.Read(ctx)
		if err != nil {
			if websocket.CloseStatus(err) == websocket.StatusNormalClosure ||
				websocket.CloseStatus(err) == websocket.StatusGoingAway {
				logger.Info("WebSocket连接关闭: %s", r.RemoteAddr)
			} else {
				logger.Error("WebSocket读取失败: %v", err)
			}
			return
		}
		logger.Info("WebSocket收到消息: %s", string(data))

		// 构造回复消息
		reply := fmt.Sprintf(`{"type":"reply","data":"收到: %s","time":"%s"}`, string(data), time.Now().Format("15:04:05"))
		err = socket.Write(ctx, msgType, []byte(reply))
		if err != nil {
			logger.Error("WebSocket写入失败: %v", err)
			return
		}
	}
}
