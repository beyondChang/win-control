package main

import (
	"server/cmd"
)

// Build info, injected via -ldflags at build time
var (
	Version = "dev"
)

func main() {
	cmd.Run()
}
