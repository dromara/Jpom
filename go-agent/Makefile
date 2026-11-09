# Copyright (c) 2019 Of Him Code Technology Studio
# Jpom is licensed under Mulan PSL v2.

GO ?= go
# 构建缓存必须位于工作区内（受限沙箱/CI 均可写）
GOCACHE ?= $(CURDIR)/.cache/build
GOTMPDIR ?= $(CURDIR)/.cache/tmp
GOMODCACHE ?= $(CURDIR)/.cache/mod
GOSUMDB ?= off
export GOCACHE GOTMPDIR GOMODCACHE GOSUMDB
VERSION ?= dev
BUILD_TIME ?= $(shell date -u +%Y-%m-%dT%H:%M:%SZ)
LDFLAGS := -s -w -X main.version=$(VERSION) -X main.buildTime=$(BUILD_TIME)
OUT := build/agent

.PHONY: all build test vet fmt lint clean package

all: build

build:
	CGO_ENABLED=0 $(GO) build -trimpath -ldflags '$(LDFLAGS)' -o $(OUT) .

test:
	$(GO) test ./...

vet:
	$(GO) vet ./...

fmt:
	gofmt -l -w .

clean:
	rm -rf build dist

package: build
	bash script/package.sh