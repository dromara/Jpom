//
// Copyright (c) 2019 Of Him Code Technology Studio
// Jpom is licensed under Mulan PSL v2.
// You can use this software according to the terms and conditions of the Mulan PSL v2.
// You may obtain a copy of Mulan PSL v2 at:
// 			http://license.coscl.org.cn/MulanPSL2
// THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
// See the Mulan PSL v2 for more details.
//

package tunnel

import (
	"bytes"
	"encoding/json"
	"fmt"
	"strconv"
)

// UnmarshalJSON 兼容服务端把数字字段序列化成字符串（以及正常数字）两种形态。
//
// 背景：服务端 fastjson2 在部分运行环境下会把 Long 写成字符串（例如 "len":"4194304"），
// 直接按 int64 解析会让插件端在握手阶段断开。这里对数值字段统一做“数字或字符串”兼容，
// 保证 Java / Go 两端协议互通。
func (f *Frame) UnmarshalJSON(data []byte) error {
	decoder := json.NewDecoder(bytes.NewReader(data))
	decoder.UseNumber()
	var raw map[string]interface{}
	if err := decoder.Decode(&raw); err != nil {
		return err
	}
	if raw == nil {
		return fmt.Errorf("empty frame")
	}
	str := func(key string) string {
		if value, ok := raw[key]; ok {
			if text, ok := value.(string); ok {
				return text
			}
		}
		return ""
	}
	num := func(key string) (int64, bool) {
		value, ok := raw[key]
		if !ok || value == nil {
			return 0, false
		}
		switch typed := value.(type) {
		case json.Number:
			parsed, err := typed.Int64()
			if err != nil {
				return 0, false
			}
			return parsed, true
		case string:
			if typed == "" {
				return 0, false
			}
			parsed, err := strconv.ParseInt(typed, 10, 64)
			if err != nil {
				return 0, false
			}
			return parsed, true
		case float64:
			return int64(typed), true
		}
		return 0, false
	}
	boolean := func(key string) (*bool, bool) {
		value, ok := raw[key]
		if !ok {
			return nil, false
		}
		if flag, ok := value.(bool); ok {
			return &flag, true
		}
		return nil, false
	}
	stringMap := func(key string) map[string]string {
		value, ok := raw[key]
		if !ok {
			return nil
		}
		rawMap, ok := value.(map[string]interface{})
		if !ok {
			return nil
		}
		result := make(map[string]string, len(rawMap))
		for k, v := range rawMap {
			if text, ok := v.(string); ok {
				result[k] = text
			} else if v != nil {
				result[k] = fmt.Sprint(v)
			}
		}
		return result
	}

	f.Type = str("type")
	f.Sid = str("sid")
	f.MachineID = str("machineId")
	f.Key = str("key")
	f.AgentVersion = str("agentVersion")
	f.OS = str("os")
	f.Arch = str("arch")
	f.Nonce = str("nonce")
	f.Sign = str("sign")
	f.SessionID = str("sessionId")
	f.ServerVersion = str("serverVersion")
	f.Msg = str("msg")
	f.Method = str("method")
	f.Path = str("path")
	f.ContentType = str("contentType")
	f.Ts, _ = num("ts")
	f.Heartbeat, _ = intOrZero(num("heartbeat"))
	f.MaxConcurrency, _ = intOrZero(num("maxConcurrency"))
	f.Code, _ = intOrZero(num("code"))
	f.Len, _ = num("len")
	f.Timeout, _ = intOrZero(num("timeout"))
	f.Status, _ = intOrZero(num("status"))
	f.N, _ = num("n")
	f.Topic = str("topic")
	f.Body = str("body")
	f.Seq, _ = num("seq")
	f.Headers = stringMap("headers")
	f.Query = stringMap("query")
	if ok, present := boolean("ok"); present {
		f.Ok = ok
	}
	if push, present := boolean("push"); present {
		f.Push = push
	}
	return nil
}

func intOrZero(value int64, ok bool) (int, bool) {
	if !ok {
		return 0, false
	}
	return int(value), true
}
