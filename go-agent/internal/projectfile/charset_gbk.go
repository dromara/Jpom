/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package projectfile

import (
	"bytes"
	"io"

	"golang.org/x/text/encoding/simplifiedchinese"
	"golang.org/x/text/transform"
)

// init installs the GBK codec used by the "suffix@GBK" editable suffix form and
// by ZIP entry names that were encoded on a Chinese Windows machine. The Java
// agent tries GBK first and falls back to UTF-8 (CompressionFileUtil), so the
// file module must be able to decode both.
func init() {
	gbkDecoder = func(data []byte) (string, error) {
		reader := transform.NewReader(bytes.NewReader(data), simplifiedchinese.GBK.NewDecoder())
		decoded, err := io.ReadAll(reader)
		if err != nil {
			return "", err
		}
		return string(decoded), nil
	}
	gbkEncoder = func(text string) ([]byte, error) {
		var buffer bytes.Buffer
		writer := transform.NewWriter(&buffer, simplifiedchinese.GBK.NewEncoder())
		if _, err := writer.Write([]byte(text)); err != nil {
			return nil, err
		}
		if err := writer.Close(); err != nil {
			return nil, err
		}
		return buffer.Bytes(), nil
	}
}
