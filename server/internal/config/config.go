package config

import (
	"fmt"
	"os"
	"path/filepath"

	"github.com/spf13/viper"
)

var dataDir string

// Init 初始化配置系统：创建数据目录，加载或生成 config.yaml
func Init() {
	appDataDir := os.Getenv("LOCALAPPDATA")
	if appDataDir == "" {
		appDataDir = os.Getenv("APPDATA")
	}
	if appDataDir == "" {
		homeDir, err := os.UserHomeDir()
		if err != nil {
			fmt.Fprintf(os.Stderr, "获取用户目录失败: %v\n", err)
			os.Exit(1)
		}
		appDataDir = filepath.Join(homeDir, "AppData", "Local")
	}

	dataDir = filepath.Join(appDataDir, "server")
	if err := os.MkdirAll(dataDir, 0755); err != nil {
		fmt.Fprintf(os.Stderr, "创建数据目录失败: %v\n", err)
		os.Exit(1)
	}

	viper.SetConfigName("config")
	viper.SetConfigType("yaml")
	viper.AddConfigPath(dataDir)

	viper.SetDefault("port", 1800)
	viper.SetDefault("auto_start", false)
	viper.SetDefault("open_on_start", true)
	viper.SetDefault("local_only", true)

	if err := viper.ReadInConfig(); err != nil {
		// 配置文件不存在，按默认值生成
		if err := viper.SafeWriteConfigAs(filepath.Join(dataDir, "config.yaml")); err != nil {
			fmt.Fprintf(os.Stderr, "写入默认配置失败: %v\n", err)
		}
	}
}

// GetPort 返回服务监听端口
func GetPort() int {
	return viper.GetInt("port")
}

// GetDataDir 返回数据目录路径
func GetDataDir() string {
	return dataDir
}

// GetBool 返回指定配置的布尔值
func GetBool(key string) bool {
	return viper.GetBool(key)
}

// Set 设置配置项并写入文件
func Set(key string, value interface{}) {
	viper.Set(key, value)
	viper.WriteConfig()
}

// GetAll 返回所有配置项
func GetAll() map[string]interface{} {
	return viper.AllSettings()
}
