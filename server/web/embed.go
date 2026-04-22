package web

import "embed"

//go:embed all:css all:js all:assets *.html
var Assets embed.FS
