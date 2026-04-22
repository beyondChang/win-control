package server

import (
	"io"
	"net/http"
	"net/http/httptest"
	"testing"

	"server/internal/handler"
	"server/internal/utils/autostart"
)

// TestHandler 测试 HelloWorld 接口返回正确的 JSON 响应
func TestHandler(t *testing.T) {
	asm := autostart.New("server")
	h := handler.New(nil, asm)
	server := httptest.NewServer(http.HandlerFunc(h.HelloWorld))
	defer server.Close()
	resp, err := http.Get(server.URL)
	if err != nil {
		t.Fatalf("error making request to server. Err: %v", err)
	}
	defer resp.Body.Close()
	if resp.StatusCode != http.StatusOK {
		t.Errorf("expected status OK; got %v", resp.Status)
	}
	expected := "{\"message\":\"Hello World\"}\n"
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		t.Fatalf("error reading response body. Err: %v", err)
	}
	if expected != string(body) {
		t.Errorf("expected response body to be %v; got %v", expected, string(body))
	}
}
