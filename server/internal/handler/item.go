package handler

import (
	"encoding/json"
	"net/http"
	"strconv"
	"server/internal/utils/logger"

	"github.com/go-chi/chi/v5"
)

// ListItems 查询并返回所有项目列表
func (h *Handler) ListItems(w http.ResponseWriter, r *http.Request) {
	items, err := h.db.GetItems()
	if err != nil {
		logger.Error("查询列表失败: %v", err)
		writeError(w, http.StatusInternalServerError, "查询失败")
		return
	}
	if items == nil {
		items = []map[string]interface{}{}
	}
	writeJSON(w, http.StatusOK, items)
}

// CreateItem 创建新项目，请求体需包含 name 字段
func (h *Handler) CreateItem(w http.ResponseWriter, r *http.Request) {
	var body struct {
		Name string `json:"name"`
	}
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil || body.Name == "" {
		writeError(w, http.StatusBadRequest, "name 不能为空")
		return
	}

	id, err := h.db.CreateItem(body.Name)
	if err != nil {
		logger.Error("创建失败: %v", err)
		writeError(w, http.StatusInternalServerError, "创建失败")
		return
	}
	logger.Info("创建项目: id=%d, name=%s", id, body.Name)
	writeJSON(w, http.StatusCreated, map[string]interface{}{"id": id, "name": body.Name})
}

// UpdateItem 根据 URL 中的 id 更新项目名称
func (h *Handler) UpdateItem(w http.ResponseWriter, r *http.Request) {
	id, err := strconv.ParseInt(chi.URLParam(r, "id"), 10, 64)
	if err != nil {
		writeError(w, http.StatusBadRequest, "无效的 id")
		return
	}

	var body struct {
		Name string `json:"name"`
	}
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil || body.Name == "" {
		writeError(w, http.StatusBadRequest, "name 不能为空")
		return
	}

	if err := h.db.UpdateItem(id, body.Name); err != nil {
		logger.Error("更新失败: %v", err)
		writeError(w, http.StatusInternalServerError, "更新失败")
		return
	}
	logger.Info("更新项目: id=%d, name=%s", id, body.Name)
	writeJSON(w, http.StatusOK, map[string]string{"message": "更新成功"})
}

// DeleteItem 根据 URL 中的 id 删除项目
func (h *Handler) DeleteItem(w http.ResponseWriter, r *http.Request) {
	id, err := strconv.ParseInt(chi.URLParam(r, "id"), 10, 64)
	if err != nil {
		writeError(w, http.StatusBadRequest, "无效的 id")
		return
	}

	if err := h.db.DeleteItem(id); err != nil {
		logger.Error("删除失败: %v", err)
		writeError(w, http.StatusInternalServerError, "删除失败")
		return
	}
	logger.Info("删除项目: id=%d", id)
	writeJSON(w, http.StatusOK, map[string]string{"message": "删除成功"})
}
