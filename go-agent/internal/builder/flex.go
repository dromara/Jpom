//
// Copyright (c) 2019 Of Him Code Technology Studio
// Jpom is licensed under Mulan PSL v2.
// You can use this software according to the terms and conditions of the Mulan PSL v2.
// You may obtain a copy of Mulan PSL v2 at:
// 			http://license.coscl.org.cn/MulanPSL2
// THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
// See the Mulan PSL v2 for more details.
//

package builder

import (
	"fmt"
	"strconv"
	"strings"
)

// flexInt / flexBool 让 job 解析对「数字被序列化成字符串」保持容错。
//
// 背景（2026-11-19 真机验证发现）：Server 通过隧道/表单转发时，JSON body 里的数值可能被
// 转成字符串（例如 "timeoutSec":"7200"），严格按 int 解析会让整个 job 直接被拒
// （json: cannot unmarshal string into Go struct field Spec.timeoutSec of type int）。
// 契约里这些字段语义仍是数值/布尔，这里只放宽「字符串形式」的兼容，不改变取值。
type flexInt int

// UnmarshalJSON 同时接受 7200、"7200"、7200.0、null。
func (f *flexInt) UnmarshalJSON(data []byte) error {
	text := strings.TrimSpace(string(data))
	if text == "" || text == "null" {
		*f = 0
		return nil
	}
	text = strings.Trim(text, "\"")
	if text == "" {
		*f = 0
		return nil
	}
	value, err := strconv.ParseFloat(text, 64)
	if err != nil {
		return fmt.Errorf("invalid number %q", text)
	}
	*f = flexInt(int64(value))
	return nil
}

type flexBool bool

// UnmarshalJSON 同时接受 true、"true"、1、"1"、false、"false"、0、null。
func (f *flexBool) UnmarshalJSON(data []byte) error {
	text := strings.TrimSpace(string(data))
	if text == "" || text == "null" {
		*f = false
		return nil
	}
	text = strings.Trim(text, "\"")
	value, err := strconv.ParseBool(strings.ToLower(text))
	if err != nil {
		return fmt.Errorf("invalid bool %q", text)
	}
	*f = flexBool(value)
	return nil
}
