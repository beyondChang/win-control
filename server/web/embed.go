package web

import "embed"

//go:embed all:js all:assets *.html
var Assets embed.FS
