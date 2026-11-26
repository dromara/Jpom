//
// Copyright (c) 2019 Of Him Code Technology Studio
// Jpom is licensed under Mulan PSL v2.
// You can use this software according to the terms and conditions of the Mulan PSL v2.
// You may obtain a copy of Mulan PSL v2 at:
// 			http://license.coscl.org.cn/MulanPSL2
// THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
// See the Mulan PSL v2 for more details.
//

package sysinfo

import (
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"time"

	"github.com/dromara/Jpom/go-agent/internal/app"
	"github.com/dromara/Jpom/go-agent/internal/netx"
	"github.com/dromara/Jpom/go-agent/internal/store"
)

// 远程版本信息接口默认地址（与 cn.keepbx.jpom.RemoteVersion 一致）。
const (
	releaseVersionsURL = "https://jpom.top/docs/release-versions.json"
	betaVersionsURL    = "https://jpom.top/docs/beta-versions.json"
	// remoteVersionCacheTTL 对应 Java 端 cacheInfo 的 24 小时过期判断。
	remoteVersionCacheTTL = 24 * time.Hour
	// remoteVersionDefaultTimeout 版本检查请求默认超时时间，对应 Java 端 RemoteVersion 的 10 秒。
	remoteVersionDefaultTimeout = 10 * time.Second
	// remoteVersionTimeoutEnv 版本检查请求超时时间（单位秒）的环境变量，与 Java 端同名。
	remoteVersionTimeoutEnv = "JPOM_REMOTE_VERSION_TIMEOUT"
)

// resolveRemoteVersionTimeout 对应 Java RemoteVersion.timeoutMillis()：读取环境变量
// JPOM_REMOTE_VERSION_TIMEOUT（单位秒，默认 10 秒），每次请求都重新读取；未配置、非数字
// 或小于等于 0 时回落默认值，并返回一条告警文案（对应 Java 端的 log.warn）。
func resolveRemoteVersionTimeout() (time.Duration, string) {
	raw := strings.TrimSpace(os.Getenv(remoteVersionTimeoutEnv))
	if raw == "" {
		return remoteVersionDefaultTimeout, ""
	}
	seconds, err := strconv.Atoi(raw)
	if err != nil || seconds <= 0 {
		return remoteVersionDefaultTimeout, fmt.Sprintf(
			"invalid %s=%q, fallback to default %d seconds",
			remoteVersionTimeoutEnv, raw, int(remoteVersionDefaultTimeout/time.Second))
	}
	return time.Duration(seconds) * time.Second, ""
}

// remoteVersionTimeoutFor 返回本次请求使用的超时时间，配置非法时落一条告警日志。
func remoteVersionTimeoutFor(a *app.App) time.Duration {
	timeout, warning := resolveRemoteVersionTimeout()
	if warning != "" && a != nil {
		a.Log().Warn(warning)
	}
	return timeout
}

// RemoteVersionFile 返回 data/remote_version.json 路径（Const.REMOTE_VERSION）。
func RemoteVersionFile(a *app.App) string { return filepath.Join(a.DataDir, "remote_version.json") }

// JoinBetaRelease 对应 RemoteVersion.betaRelease()，读取环境变量 JOIN_JPOM_BETA_RELEASE。
func JoinBetaRelease() bool {
	value := strings.TrimSpace(os.Getenv("JOIN_JPOM_BETA_RELEASE"))
	if value == "" {
		return false
	}
	switch strings.ToLower(value) {
	case "true", "1", "yes", "y", "on":
		return true
	default:
		return false
	}
}

func defaultRemoteVersionURL() string {
	if JoinBetaRelease() {
		return betaVersionsURL
	}
	return releaseVersionsURL
}

// RemoteVersionURL 返回配置的远程版本地址，未配置时使用官方默认地址。
func RemoteVersionURL(a *app.App) string {
	if a != nil && a.Config != nil && strings.TrimSpace(a.Config.Jpom.System.RemoteVersionURL) != "" {
		return strings.TrimSpace(a.Config.Jpom.System.RemoteVersionURL)
	}
	return defaultRemoteVersionURL()
}

// CacheInfo 对应 RemoteVersion.cacheInfo()：读取缓存文件，超过 24 小时视为过期。
func CacheInfo(a *app.App) map[string]any {
	content, err := os.ReadFile(RemoteVersionFile(a))
	if err != nil || len(content) == 0 {
		return nil
	}
	var value map[string]any
	if err := json.Unmarshal(content, &value); err != nil {
		a.Log().Warn("parse remote version failed", "error", err)
		return nil
	}
	lastTime := toInt64(value["lastTime"])
	if lastTime <= 0 || time.Since(time.UnixMilli(lastTime)) >= remoteVersionCacheTTL {
		return nil
	}
	return value
}

// remoteVersionFetchAttempts 在线刷新尝试次数：首次失败自动重试，与 Java 端
// RemoteVersionResolver（DEFAULT_FETCH_ATTEMPTS=3）保持一致。
const remoteVersionFetchAttempts = 3

// remoteVersionRetryInterval 重试间隔，与 Java 端 DEFAULT_RETRY_INTERVAL_MILLIS=500 一致。
const remoteVersionRetryInterval = 500 * time.Millisecond

// errNoRemoteVersionInfo 拉取成功但内容不可用（没有 tagName）时的错误。
var errNoRemoteVersionInfo = errors.New("no remote version information available")

// ResolveRemoteInfo 带刷新语义的远端版本信息解析：在线刷新（失败自动重试）→ 本机缓存兜底。
//
// 对应 Java 端 RemoteVersions#infoForUpgrade / infoForRefresh：此前各入口只调一次
// LoadRemoteInfo，下载中心偶发 TLS 抖动就直接失败（升级/下载返回 405），且没有任何兜底。
func ResolveRemoteInfo(a *app.App) (map[string]any, error) {
	return resolveRemoteInfo(
		func() (map[string]any, error) { return LoadRemoteInfo(a) },
		func() map[string]any { return CacheInfo(a) },
		time.Sleep,
		func(msg string) { a.Log().Warn(msg) },
	)
}

// resolveRemoteInfo 是 ResolveRemoteInfo 的策略内核（不依赖 App，便于单测）：
// 在线刷新最多 remoteVersionFetchAttempts 次 → 本机缓存兜底 → 返回最后一次错误。
func resolveRemoteInfo(fetch func() (map[string]any, error), cached func() map[string]any,
	sleep func(time.Duration), log func(msg string)) (map[string]any, error) {
	var lastErr error
	for attempt := 0; attempt < remoteVersionFetchAttempts; attempt++ {
		if attempt > 0 && sleep != nil {
			sleep(remoteVersionRetryInterval)
		}
		value, err := fetch()
		if err == nil && value != nil && strings.TrimSpace(toString(value["tagName"])) != "" {
			return value, nil
		}
		if err == nil {
			err = errNoRemoteVersionInfo
		}
		lastErr = err
		if log != nil {
			log(fmt.Sprintf("fetch remote version info failed, attempt %d/%d: %v", attempt+1, remoteVersionFetchAttempts, err))
		}
	}
	if cached != nil {
		if value := cached(); value != nil {
			if log != nil {
				log(fmt.Sprintf("remote version info unavailable, fallback to local cache: %v", value["tagName"]))
			}
			return value, nil
		}
	}
	return nil, lastErr
}

// LoadRemoteInfo 对应 RemoteVersion.loadRemoteInfo()：拉取远程版本信息、计算
// upgrade/changelog 并写入 data/remote_version.json。
func LoadRemoteInfo(a *app.App) (map[string]any, error) {
	url := RemoteVersionURL(a)
	if !strings.HasPrefix(url, "http://") && !strings.HasPrefix(url, "https://") {
		url = defaultRemoteVersionURL()
	}
	value, err := loadTransitURL(a.UserAgent(), url, remoteVersionTimeoutFor(a))
	if err != nil {
		return nil, err
	}
	if value == nil || strings.TrimSpace(toString(value["tagName"])) == "" {
		return nil, fmt.Errorf("no remote version information available")
	}
	cacheLoadTime(a, value)
	return value, nil
}

func loadTransitURL(userAgent, url string, timeout time.Duration) (map[string]any, error) {
	client := netx.Client(userAgent, timeout)
	response, err := client.Get(url)
	if err != nil {
		return nil, err
	}
	defer func() { _ = response.Body.Close() }()
	body, err := io.ReadAll(response.Body)
	if err != nil {
		return nil, err
	}
	var value map[string]any
	if err := json.Unmarshal(body, &value); err != nil {
		return nil, fmt.Errorf("parse remote version failed: %w", err)
	}
	if value == nil {
		return nil, nil
	}
	normalizeRemoteVersionAliases(value)
	if toString(value["tagName"]) != "" && toString(value["agentUrl"]) != "" && toString(value["serverUrl"]) != "" {
		return value, nil
	}
	// 中转地址：远程返回 { "url": "..." } 时继续拉取。
	if transit := strings.TrimSpace(toString(value["url"])); transit != "" {
		return loadTransitURL(userAgent, transit, timeout)
	}
	return nil, nil
}

// normalizeRemoteVersionAliases 兼容上游 JSON 的下划线别名（tag_name 等），
// 统一成 Java RemoteVersion 的驼峰字段名。
func normalizeRemoteVersionAliases(value map[string]any) {
	aliases := map[string]string{
		"tag_name":        "tagName",
		"agent_url":       "agentUrl",
		"server_url":      "serverUrl",
		"changelog_url":   "changelogUrl",
		"last_time":       "lastTime",
		"download_source": "downloadSource",
	}
	for alias, canonical := range aliases {
		if toString(value[canonical]) != "" {
			continue
		}
		if item, ok := value[alias]; ok && item != nil {
			value[canonical] = item
		}
	}
}

// remoteVersionCanonicalFields 是 Java RemoteVersion / RemoteVersionAuth 序列化时
// 会输出的字段集合，缓存文件与 HTTP 响应都必须严格遵守它（避免上游 JSON 的
// downloadUrl/tag_name/url 等别名污染 data/remote_version.json）。
var remoteVersionCanonicalFields = []string{
	"tagName", "agentUrl", "serverUrl", "changelogUrl", "changelog",
	"lastTime", "upgrade", "beta", "downloadSource", "auth",
}

var remoteVersionAuthFields = []string{"downloadSource", "agentUrl", "serverUrl"}

// CanonicalRemoteVersion 把上游远程版本 JSON 映射/裁剪为 Java RemoteVersion 的字段集：
//   - 先兼容 tag_name/agent_url 等下划线别名；
//   - 只保留 Java 会序列化的字段；
//   - auth 同样只保留 Java RemoteVersionAuth 的三个字段；
//   - nil 值不输出（Jackson NON_NULL 语义）。
func CanonicalRemoteVersion(value map[string]any) map[string]any {
	normalizeRemoteVersionAliases(value)
	result := make(map[string]any, len(remoteVersionCanonicalFields))
	for _, key := range remoteVersionCanonicalFields {
		item, ok := value[key]
		if !ok || item == nil {
			continue
		}
		if key == "auth" {
			if auth, ok := item.(map[string]any); ok {
				trimmed := make(map[string]any, len(remoteVersionAuthFields))
				for _, authKey := range remoteVersionAuthFields {
					if authItem, exists := auth[authKey]; exists && authItem != nil {
						trimmed[authKey] = authItem
					}
				}
				if len(trimmed) == 0 {
					continue
				}
				result[key] = trimmed
				continue
			}
		}
		result[key] = item
	}
	return result
}

// cacheLoadTime 对应 RemoteVersion.cacheLoadTime。
func cacheLoadTime(a *app.App, value map[string]any) {
	now := time.Now().UnixMilli()
	value["lastTime"] = now
	upgrade := false
	if a.Version != "dev" {
		if compareVersion(strings.TrimPrefix(toString(value["tagName"]), "v"), a.Version) > 0 {
			upgrade = true
		}
	}
	if toString(value["agentUrl"]) == "" {
		upgrade = false
	}
	value["upgrade"] = upgrade
	if changelogURL := strings.TrimSpace(toString(value["changelogUrl"])); changelogURL != "" {
		// changelog 属附加信息：获取失败（含超时）只告警，不影响版本检查结果（与 Java 端一致）。
		if changelog, err := fetchText(a.UserAgent(), changelogURL, remoteVersionTimeoutFor(a)); err == nil {
			value["changelog"] = changelog
		} else {
			a.Log().Warn("fetch remote version changelog failed", "error", err)
		}
	}
	// 与 Java 缓存文件一致：只写 RemoteVersion 字段集（别名/额外字段全部丢弃）。
	content, err := json.Marshal(CanonicalRemoteVersion(value))
	if err != nil {
		return
	}
	if err := store.New(RemoteVersionFile(a)).WriteRaw(content); err != nil {
		a.Log().Warn("write remote version cache failed", "error", err)
	}
}

func fetchText(userAgent, url string, timeout time.Duration) (string, error) {
	client := netx.Client(userAgent, timeout)
	response, err := client.Get(url)
	if err != nil {
		return "", err
	}
	defer func() { _ = response.Body.Close() }()
	body, err := io.ReadAll(response.Body)
	if err != nil {
		return "", err
	}
	return string(body), nil
}

// ChangeLog 对应 SystemUpdateController.changeLog：beta 参数或 betaRelease 时
// 返回 BETA 日志，否则返回正式版日志。Go 端优先读取安装目录下的日志文件。
func ChangeLog(a *app.App, beta bool) string {
	names := []string{"CHANGELOG.md"}
	if JoinBetaRelease() || beta {
		names = []string{"CHANGELOG-BETA.md", "CHANGELOG.md"}
	}
	for _, name := range names {
		for _, dir := range []string{a.BaseDir, filepath.Join(a.BaseDir, "conf"), a.RunDir} {
			path := filepath.Join(dir, name)
			if content, err := os.ReadFile(path); err == nil {
				return string(content)
			}
		}
	}
	// 回退到远程版本缓存里的 changelog（Java 端为内置资源，Go 发布包建议带上
	// CHANGELOG.md；缺失时至少返回缓存中的内容）。
	if cached := CacheInfo(a); cached != nil {
		return toString(cached["changelog"])
	}
	return ""
}

func toString(value any) string {
	switch typed := value.(type) {
	case string:
		return typed
	case json.Number:
		return typed.String()
	default:
		if value == nil {
			return ""
		}
		return fmt.Sprintf("%v", value)
	}
}

func toInt64(value any) int64 {
	switch typed := value.(type) {
	case json.Number:
		parsed, err := typed.Int64()
		if err == nil {
			return parsed
		}
	case float64:
		return int64(typed)
	case int64:
		return typed
	case int:
		return int64(typed)
	case string:
		parsed, err := strconv.ParseInt(strings.TrimSpace(typed), 10, 64)
		if err == nil {
			return parsed
		}
	}
	return 0
}

// compareVersion 近似 hutool StrUtil.compareVersion：按 "." 分段比较，缺失段按 0 处理。
func compareVersion(left, right string) int {
	leftParts := strings.Split(strings.TrimSpace(left), ".")
	rightParts := strings.Split(strings.TrimSpace(right), ".")
	length := len(leftParts)
	if len(rightParts) > length {
		length = len(rightParts)
	}
	for index := 0; index < length; index++ {
		leftValue := "0"
		if index < len(leftParts) {
			leftValue = leftParts[index]
		}
		rightValue := "0"
		if index < len(rightParts) {
			rightValue = rightParts[index]
		}
		leftNumber, leftErr := strconv.Atoi(leftValue)
		rightNumber, rightErr := strconv.Atoi(rightValue)
		if leftErr == nil && rightErr == nil {
			if leftNumber != rightNumber {
				if leftNumber > rightNumber {
					return 1
				}
				return -1
			}
			continue
		}
		if leftValue != rightValue {
			if leftValue > rightValue {
				return 1
			}
			return -1
		}
	}
	return 0
}
