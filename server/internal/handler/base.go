package handler

import (
	"net/http"
	"server/internal/database"
	"server/internal/utils/autostart"
)

// Handler 结构体，持有依赖服务
type Handler struct {
	db database.Service
	autostartManager autostart.Manager
}

// New 创建 Handler 实例，注入依赖服务
func New(db database.Service, asm autostart.Manager) *Handler {
	return &Handler{
		db: db,
		autostartManager: asm,
	}
}

// HelloWorld 返回 Hello World 测试响应
func (h *Handler) HelloWorld(w http.ResponseWriter, r *http.Request) {
	writeJSON(w, http.StatusOK, map[string]string{"message": "Hello World"})
}

// Health 返回数据库健康状态信息
func (h *Handler) Health(w http.ResponseWriter, r *http.Request) {
	writeJSON(w, http.StatusOK, h.db.Health())
}
