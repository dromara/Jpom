/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

// Package i18n 提供与 Java 插件端一致的国际化文案（zh-CN / zh-HK / zh-TW / en-US）。
//
// 数据来源：jpom-parent/modules/common/src/main/resources/i18n/messages_{zh_CN,zh_HK,zh_TW,en_US}.properties，
// 由 script/gen-i18n.py 生成 messages.gen.json（key -> {lang: 文案}）后 go:embed 进二进制。
// Java 的占位符为 {}（可多个），Go 端通过 T(lang, key, args...) 顺序替换。
//
// 常用接口：
//
//	i18n.T(lang, key, args...)             渲染文案（Java {} 顺序替换）
//	i18n.Lookup(lang, key)                 取单语言文案（不替换占位符）
//	i18n.Get(key)                          取 key 的全部语言条目
//	i18n.Has(key) / i18n.Keys()            存在性/全部 key（检查工具用）
//	i18n.NormalizeLang(lang)               归一化语言标签
//	i18n.BestLanguage(acceptLanguage)      从 Accept-Language 头按 q 权重选语言
//	i18n.Resolve(acceptLanguage, fallback) 按 Java 优先级解析请求语言
//	i18n.DefaultLang()                     配置语言（JPOM_LANG），未配置 zh-CN
//	httpx.Context.T(key, args...)          当前请求语言下的文案（推荐 handler 使用）
package i18n

import (
	_ "embed"
	"encoding/json"
	"fmt"
	"os"
	"sort"
	"strconv"
	"strings"
	"sync"
)

// 支持的语言标识。
const (
	LangZhCn = "zh-CN"
	LangZhHk = "zh-HK"
	LangZhTw = "zh-TW"
	LangEnUs = "en-US"

	// 简写别名，便于调用方书写。
	ZhCN = LangZhCn
	ZhHK = LangZhHk
	ZhTW = LangZhTw
	EnUS = LangEnUs
)

// LanguageEnv 是 Java 端默认语言配置来源（jpom.system.lang 会设置该变量）。
const LanguageEnv = "JPOM_LANG"

//go:embed messages.gen.json
var messagesJSON []byte

//go:embed messages_go.json
var messagesGoJSON []byte

var (
	once     sync.Once
	messages map[string]map[string]string
)

func load() {
	once.Do(func() {
		messages = map[string]map[string]string{}
		if err := json.Unmarshal(messagesJSON, &messages); err != nil {
			// 数据由构建期脚本生成，正常不会失败；失败时所有 T 回退为 key。
			messages = map[string]map[string]string{}
		}
		// Go 版新增文案（Java 端不存在的升级/回滚相关提示）叠加到同一张表，
		// 已有 key 以 Java properties 为准，不覆盖。
		overlay := map[string]map[string]string{}
		if err := json.Unmarshal(messagesGoJSON, &overlay); err == nil {
			for key, entry := range overlay {
				if _, exists := messages[key]; exists {
					continue
				}
				messages[key] = entry
			}
		}
	})
}

// NormalizeLang 归一化语言标签，返回支持的语言（zh-CN/zh-HK/zh-TW/en-US）。
//
// 兼容大小写、下划线、地区变体与逗号权重：取第一个语言标签，如
// "zh-CN,zh;q=0.9" -> zh-CN。未知语言回退 zh-CN（与 Java 一致）。
func NormalizeLang(value string) string {
	value = strings.TrimSpace(value)
	if value == "" {
		return LangZhCn
	}
	tag := strings.ToLower(strings.TrimSpace(strings.Split(value, ",")[0]))
	tag = strings.TrimSpace(strings.Split(tag, ";")[0])
	tag = strings.ReplaceAll(tag, "_", "-")
	switch {
	case strings.HasPrefix(tag, "zh-hk") || strings.HasPrefix(tag, "zh-hant-hk"):
		return LangZhHk
	case strings.HasPrefix(tag, "zh-tw") || strings.HasPrefix(tag, "zh-hant-tw") || strings.HasPrefix(tag, "zh-hant"):
		return LangZhTw
	case strings.HasPrefix(tag, "zh"):
		return LangZhCn
	case strings.HasPrefix(tag, "en"):
		return LangEnUs
	default:
		return LangZhCn
	}
}

// Has 判断 key 是否存在。
func Has(key string) bool {
	load()
	_, ok := messages[key]
	return ok
}

// Lookup 返回指定语言的文案（不做占位符替换）；缺失时回退 zh-CN，仍缺失返回 key。
func Lookup(lang, key string) string {
	if key == "" {
		return ""
	}
	load()
	entry, ok := messages[key]
	if !ok {
		return key
	}
	if text := entry[NormalizeLang(lang)]; text != "" {
		return text
	}
	if text := entry[LangZhCn]; text != "" {
		return text
	}
	if text := entry[LangEnUs]; text != "" {
		return text
	}
	return key
}

// T 返回指定语言文案，并按 Java StrUtil.format 语义顺序替换 {} 占位符
// （参数不足时保留剩余 {}，与 Java 一致）。
func T(lang, key string, args ...any) string {
	value := Lookup(lang, key)
	if len(args) == 0 {
		return value
	}
	for _, arg := range args {
		if !strings.Contains(value, "{}") {
			break
		}
		value = strings.Replace(value, "{}", formatArg(arg), 1)
	}
	return value
}

// Ts 是 T 的别名。
func Ts(lang, key string, args ...any) string { return T(lang, key, args...) }

// Sprintf 与 T 等价，保留给需要显式格式化的调用方。
func Sprintf(lang, key string, args ...any) string { return T(lang, key, args...) }

// Int 把语言标签归一化（部分调用方按 Java Locale 风格使用）。
func Int(lang string) string { return NormalizeLang(lang) }

// Get 返回 key 的完整多语言条目（测试/检查工具用；不存在返回 nil）。
func Get(key string) map[string]string {
	load()
	return messages[key]
}

// Keys 返回全部 key（按字典序，检查工具用）。
func Keys() []string {
	load()
	keys := make([]string, 0, len(messages))
	for key := range messages {
		keys = append(keys, key)
	}
	sort.Strings(keys)
	return keys
}

// Languages 返回支持的语言列表。
func Languages() []string { return []string{LangZhCn, LangZhHk, LangZhTw, LangEnUs} }

// MissingKeys 返回指定语言下缺失文案的 key（按字典序），用于检查工具/门禁。
func MissingKeys(lang string) []string {
	normalized := NormalizeLang(lang)
	load()
	result := make([]string, 0, 16)
	for key, entry := range messages {
		if entry[normalized] == "" {
			result = append(result, key)
		}
	}
	sort.Strings(result)
	return result
}

// DefaultLang 返回配置的默认语言：优先 JPOM_LANG 环境变量（对应
// jpom.system.lang），其次系统 locale（LC_ALL/LANG），最后 zh-CN。
func DefaultLang() string {
	if value := strings.TrimSpace(os.Getenv(LanguageEnv)); value != "" {
		return NormalizeLang(value)
	}
	return SystemLanguage()
}

// SystemLanguage 把系统 locale（LC_ALL 优先于 LANG）映射为支持的语言：
// zh_* -> zh-CN/zh-HK/zh-TW，en_* -> en-US，其它/未配置 -> zh-CN。
// 对应 Java I18nMessageUtil 中 Locale.getDefault() 的回退分支。
func SystemLanguage() string {
	for _, name := range []string{"LC_ALL", "LC_MESSAGES", "LANG"} {
		value := strings.TrimSpace(os.Getenv(name))
		if value == "" {
			continue
		}
		// 去掉 ".UTF-8" 编码后缀与 "@modifier"。
		if index := strings.IndexAny(value, ".@"); index > 0 {
			value = value[:index]
		}
		tag := strings.ToLower(strings.ReplaceAll(strings.TrimSpace(value), "_", "-"))
		switch {
		case strings.HasPrefix(tag, "zh-hk") || strings.HasPrefix(tag, "zh-hant-hk"):
			return LangZhHk
		case strings.HasPrefix(tag, "zh-tw") || strings.HasPrefix(tag, "zh-hant-tw") || strings.HasPrefix(tag, "zh-hant"):
			return LangZhTw
		case strings.HasPrefix(tag, "zh"):
			return LangZhCn
		case strings.HasPrefix(tag, "en"):
			return LangEnUs
		case tag == "c" || tag == "posix":
			continue
		}
	}
	return LangZhCn
}

// Resolve 按 Java 端的语言优先级解析请求语言：
//  1. Accept-Language（按 q 权重，权重相同取标签字典序最大者）；
//  2. 显式 fallback（通常是 jpom.system.lang 配置）；
//  3. JPOM_LANG 环境变量；
//  4. 系统 locale（LC_ALL/LANG）；
//  5. zh-CN。
func Resolve(acceptLanguage, fallback string) string {
	if best := BestLanguage(acceptLanguage); best != "" {
		return NormalizeLang(best)
	}
	if strings.TrimSpace(fallback) != "" {
		return NormalizeLang(fallback)
	}
	return DefaultLang()
}

// BestLanguage 从 Accept-Language 头解析权重最高的语言标签（原样返回，可能为空）。
func BestLanguage(header string) string {
	header = strings.TrimSpace(header)
	if header == "" {
		return ""
	}
	type candidate struct {
		tag     string
		quality float64
	}
	candidates := make([]candidate, 0, 4)
	for _, item := range strings.Split(header, ",") {
		item = strings.TrimSpace(item)
		if item == "" {
			continue
		}
		parts := strings.Split(item, ";")
		tag := strings.TrimSpace(parts[0])
		if tag == "" {
			continue
		}
		quality := 1.0
		for _, extra := range parts[1:] {
			extra = strings.TrimSpace(extra)
			if strings.HasPrefix(extra, "q=") {
				if parsed, err := strconv.ParseFloat(strings.TrimSpace(extra[2:]), 64); err == nil {
					quality = parsed
				}
			}
		}
		candidates = append(candidates, candidate{tag: tag, quality: quality})
	}
	if len(candidates) == 0 {
		return ""
	}
	// 与 Java 一致：先按 q 降序，再按标签降序（字典序大者优先）。
	sort.SliceStable(candidates, func(i, j int) bool {
		if candidates[i].quality != candidates[j].quality {
			return candidates[i].quality > candidates[j].quality
		}
		return candidates[i].tag > candidates[j].tag
	})
	return candidates[0].tag
}

func formatArg(value any) string {
	switch typed := value.(type) {
	case nil:
		return "null"
	case string:
		return typed
	case error:
		return typed.Error()
	case []byte:
		return string(typed)
	default:
		return fmt.Sprint(typed)
	}
}
