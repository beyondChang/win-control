package handler

import (
	"encoding/json"
	"net/http"

	"server/internal/config"
	"server/internal/utils/logger"
)

// ListSettings 查询并返回所有配置项
func (h *Handler) ListSettings(w http.ResponseWriter, r *http.Request) {
	settings := config.GetAll()
	writeJSON(w, http.StatusOK, settings)
}

// UpdateSettings 批量更新配置项，请求体为 key-value 键值对
// 特殊处理 auto_start：同时注册或取消注册开机自启动
func (h *Handler) UpdateSettings(w http.ResponseWriter, r *http.Request) {
	var body map[string]string
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
		writeError(w, http.StatusBadRequest, "无效的请求数据")
		return
	}

	for key, value := range body {
		config.Set(key, value)
	}
	// 处理 auto_start 配置变更：注册或取消注册开机自启动
	if val, ok := body["auto_start"]; ok {
		if val == "true" {
			if err := h.autostartManager.Enable(); err != nil {
				logger.Error("注册开机自启动失败: %v", err)
			}
		} else {
			if err := h.autostartManager.Disable(); err != nil {
				logger.Error("取消开机自启动失败: %v", err)
			}
		}
	}
	logger.Info("配置已更新: %v", body)
	writeJSON(w, http.StatusOK, map[string]string{"message": "配置已保存"})
}
