/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package script

import (
	"encoding/json"
	"fmt"
	"strconv"
	"strings"
	"unicode"

	"github.com/dromara/Jpom/go-agent/internal/i18n"
)

// CommandParam is one script argument as stored by Jpom
// (cn.keepbx CommandParam: value + desc).
type CommandParam struct {
	Value string `json:"value"`
	Desc  string `json:"desc"`
}

// ConvertToParam normalizes the script defArgs value, mirroring
// CommandParam.convertToParam: legacy space separated values become a JSON
// array of {value, desc}, JSON arrays/objects are kept as JSON.
func ConvertToParam(defArgs string) string {
	trimmed := strings.TrimSpace(defArgs)
	if trimmed == "" {
		return "[]"
	}
	switch detectJSONType(trimmed) {
	case jsonTypeObject, jsonTypeArray:
		return trimmed
	default:
		// 旧版本的数据（空格分隔的参数）
		params := ConvertLineStr(defArgs)
		encoded, err := marshalOrderedArray(params)
		if err != nil {
			return defArgs
		}
		return encoded
	}
}

// jsonType is a coarse JSON validator type, matching fastjson2
// JSONValidator.Type.Object / .Value semantics.
type jsonType int

const (
	jsonTypeInvalid jsonType = iota
	jsonTypeValue
	jsonTypeObject
	jsonTypeArray
)

func detectJSONType(value string) jsonType {
	if value == "" {
		return jsonTypeInvalid
	}
	var decoded any
	decoder := json.NewDecoder(strings.NewReader(value))
	decoder.UseNumber()
	if err := decoder.Decode(&decoded); err != nil {
		return jsonTypeInvalid
	}
	switch decoded.(type) {
	case map[string]any:
		return jsonTypeObject
	case []any:
		return jsonTypeArray
	default:
		return jsonTypeValue
	}
}

// ToCommandList converts the args value into the process argument list
// (CommandParam.toCommandList). Legacy values are split on whitespace, JSON
// arrays take the value of every entry.
func ToCommandList(params string) []string {
	trimmed := strings.TrimSpace(params)
	if trimmed == "" {
		return nil
	}
	switch detectJSONType(trimmed) {
	case jsonTypeArray:
		items := make([]CommandParam, 0, 4)
		if err := json.Unmarshal([]byte(trimmed), &items); err != nil {
			// 数组元素不是 {value, desc} 结构时按字符串数组处理
			var values []string
			if err2 := json.Unmarshal([]byte(trimmed), &values); err2 == nil {
				return values
			}
			return nil
		}
		result := make([]string, 0, len(items))
		for _, item := range items {
			if item.Value != "" {
				result = append(result, item.Value)
			}
		}
		return result
	case jsonTypeObject:
		return nil
	default:
		return splitTrim(params)
	}
}

// ConvertLineStr splits legacy space separated arguments into CommandParam
// entries (参数1, 参数2 ...).
func ConvertLineStr(defArgs string) []CommandParam {
	values := splitTrim(defArgs)
	result := make([]CommandParam, 0, len(values))
	for index, value := range values {
		result = append(result, CommandParam{Value: value, Desc: i18n.T(i18n.DefaultLang(), "i18n.parameter.3d0a") + strconv.Itoa(index+1)})
	}
	return result
}

// splitTrim splits on any whitespace and drops empty parts (Hutool splitTrim).
func splitTrim(value string) []string {
	fields := strings.FieldsFunc(value, func(r rune) bool {
		return unicode.IsSpace(r)
	})
	return fields
}

func marshalOrderedArray(params []CommandParam) (string, error) {
	var builder strings.Builder
	builder.WriteByte('[')
	for index, param := range params {
		if index > 0 {
			builder.WriteByte(',')
		}
		builder.WriteString(`{"value":`)
		encoded, err := json.Marshal(param.Value)
		if err != nil {
			return "", err
		}
		builder.Write(encoded)
		builder.WriteString(`,"desc":`)
		encoded, err = json.Marshal(param.Desc)
		if err != nil {
			return "", err
		}
		builder.Write(encoded)
		builder.WriteByte('}')
	}
	builder.WriteByte(']')
	return builder.String(), nil
}

// TriggerParamMap converts the trigger parameters into environment variables:
// every key is prefixed with trigger_ and converted to underline case
// (ScriptController.exec / NodeScriptServer.execute). A malformed params value
// returns an error (Java JSONObject.parseObject throws JSONException -> code 500).
func TriggerParamMap(params string) (map[string]string, error) {
	trimmed := strings.TrimSpace(params)
	if trimmed == "" {
		return nil, nil
	}
	decoded := map[string]any{}
	if err := json.Unmarshal([]byte(trimmed), &decoded); err != nil {
		return nil, err
	}
	result := make(map[string]string, len(decoded))
	for key, value := range decoded {
		name := toUnderlineCase("trigger_" + key)
		result[name] = toString(value)
	}
	return result, nil
}

func toString(value any) string {
	switch item := value.(type) {
	case nil:
		return ""
	case string:
		return item
	case json.Number:
		return item.String()
	case bool:
		if item {
			return "true"
		}
		return "false"
	case float64:
		return fmt.Sprintf("%v", item)
	default:
		encoded, err := json.Marshal(item)
		if err != nil {
			return fmt.Sprintf("%v", item)
		}
		return string(encoded)
	}
}

// toUnderlineCase mirrors Hutool StrUtil.toUnderlineCase: camelCase is converted
// to snake_case while existing underscore separated UPPER parts
// (trigger_BUILD_ID) are preserved, which is required for the trigger
// environment variable names used by the Jpom build triggers.
func toUnderlineCase(value string) string {
	var builder strings.Builder
	runes := []rune(value)
	for index, current := range runes {
		if unicode.IsUpper(current) {
			var previous rune
			if index > 0 {
				previous = runes[index-1]
			}
			var next rune
			if index < len(runes)-1 {
				next = runes[index+1]
			}
			if previous != 0 && unicode.IsUpper(previous) {
				// 连续大写：保持大写
				builder.WriteRune(current)
			} else if next != 0 && unicode.IsUpper(next) {
				builder.WriteRune(current)
			} else {
				if previous != 0 && previous != '_' {
					builder.WriteByte('_')
				}
				builder.WriteRune(unicode.ToLower(current))
			}
			continue
		}
		builder.WriteRune(current)
	}
	return builder.String()
}
