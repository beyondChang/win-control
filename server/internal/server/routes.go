package server

import (
	"io/fs"
	"net/http"

	"server/internal/handler"

	"github.com/go-chi/chi/v5"
	"github.com/go-chi/chi/v5/middleware"
	"github.com/go-chi/cors"
)

// WebAssets 前端静态资源，需在 RegisterRoutes 之前设置
var WebAssets fs.FS

// RegisterRoutes 注册所有路由
func (s *Server) RegisterRoutes() http.Handler {
	r := chi.NewRouter()
	r.Use(middleware.Logger)

	r.Use(cors.Handler(cors.Options{
		AllowedOrigins:   []string{"https://*", "http://*"},
		AllowedMethods:   []string{"GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"},
		AllowedHeaders:   []string{"Accept", "Authorization", "Content-Type"},
		AllowCredentials: true,
		MaxAge:           300,
	}))

	h := s.handler
	ctrl := handler.GetControlHandler()

	// 启动控制服务
	ctrl.Start()

	r.Route("/api", func(r chi.Router) {
		// 基础接口
		r.Get("/", h.HelloWorld)
		r.Get("/health", h.Health)
		r.Get("/settings", h.ListSettings)
		r.Put("/settings", h.UpdateSettings)

		// CRUD 示例
		r.Get("/items", h.ListItems)
		r.Post("/items", h.CreateItem)
		r.Put("/items/{id}", h.UpdateItem)
		r.Delete("/items/{id}", h.DeleteItem)

		// WebSocket 示例
		r.Get("/websocket", h.WebSocket)

		// 远程控制接口
		r.Get("/control/status", ctrl.HandleStatus)
		r.Post("/control/test", ctrl.HandleTest)
		r.Get("/control/events", ctrl.HandleEvents)
	})

	// WebSocket 远程控制
	r.Get("/ws/control", ctrl.HandleControlWS)

	// 静态文件
	fileServer := http.FileServer(http.FS(WebAssets))
	r.Get("/*", func(w http.ResponseWriter, r *http.Request) {
		fileServer.ServeHTTP(w, r)
	})

	return r
}
