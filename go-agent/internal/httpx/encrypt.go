/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package httpx

import (
	"bytes"
	"crypto/aes"
	"encoding/base64"
	"encoding/hex"
	"errors"
	"io"
	"mime"
	"mime/multipart"
	"net/http"
	"net/url"
	"os"
	"strings"
)

// 传输加密（对应 Java 端 DecryptionFilter）：
// transport-encryption: 0 不加密（默认）、1 BASE64、2 AES/ECB/PKCS5Padding（密钥取
// JPOM_ENCRYPT_AES_KEY，缺省与 Java 端 AESEncryptor 默认值一致）。
const (
	// defaultAESKey 是 Java 端 AESEncryptor 的默认密钥（可用环境变量覆盖）。
	defaultAESKey = "Djnn3runZBzdv9Nv"
)

var errInvalidCipher = errors.New("invalid cipher text")

type encryptor interface {
	decrypt(value string) (string, error)
}

type noneEncryptor struct{}

func (noneEncryptor) decrypt(value string) (string, error) { return value, nil }

type base64Encryptor struct{}

func (base64Encryptor) decrypt(value string) (string, error) {
	decoded, err := base64.StdEncoding.DecodeString(value)
	if err != nil {
		return "", err
	}
	return string(decoded), nil
}

type aesEncryptor struct{ key []byte }

func (e aesEncryptor) decrypt(value string) (string, error) {
	data, err := hex.DecodeString(value)
	if err != nil {
		return "", err
	}
	block, err := aes.NewCipher(e.key)
	if err != nil {
		return "", err
	}
	if len(data) == 0 || len(data)%block.BlockSize() != 0 {
		return "", errInvalidCipher
	}
	out := make([]byte, len(data))
	for start := 0; start < len(data); start += block.BlockSize() {
		block.Decrypt(out[start:start+block.BlockSize()], data[start:start+block.BlockSize()])
	}
	return string(pkcs5Unpad(out)), nil
}

func pkcs5Unpad(data []byte) []byte {
	if len(data) == 0 {
		return data
	}
	padding := int(data[len(data)-1])
	if padding <= 0 || padding > len(data) {
		return data
	}
	return data[:len(data)-padding]
}

func newEncryptor(header string) encryptor {
	switch strings.TrimSpace(header) {
	case "1":
		return base64Encryptor{}
	case "2":
		key := os.Getenv("JPOM_ENCRYPT_AES_KEY")
		if key == "" {
			key = defaultAESKey
		}
		return aesEncryptor{key: []byte(key)}
	default:
		return noneEncryptor{}
	}
}

// decryptValue decrypts a single parameter value; on failure the raw value is
// kept (the Java wrapper logs the error and continues).
func decryptValue(enc encryptor, value string) string {
	plain, err := enc.decrypt(value)
	if err != nil {
		return value
	}
	return plain
}

// decryptRequest rewrites the request in place so handlers can read plain
// parameter names/values, matching the Java DecryptionFilter behaviour.
func decryptRequest(req *http.Request) {
	enc := newEncryptor(req.Header.Get("transport-encryption"))
	if _, ok := enc.(noneEncryptor); ok {
		return
	}
	// 1) query string（GET 下载等接口的参数也走加密）
	if req.URL != nil && req.URL.RawQuery != "" {
		if values, err := url.ParseQuery(req.URL.RawQuery); err == nil {
			decrypted := url.Values{}
			for key, items := range values {
				newKey := decryptValue(enc, key)
				for _, item := range items {
					decrypted.Add(newKey, decryptValue(enc, item))
				}
			}
			req.URL.RawQuery = decrypted.Encode()
		}
	}
	contentType := req.Header.Get("Content-Type")
	mediaType, _, _ := mime.ParseMediaType(contentType)
	switch {
	case strings.EqualFold(mediaType, "application/json"):
		body, err := io.ReadAll(req.Body)
		if err != nil {
			return
		}
		_ = req.Body.Close()
		plain := decryptValue(enc, string(body))
		req.Body = io.NopCloser(bytes.NewReader([]byte(plain)))
		req.ContentLength = int64(len(plain))
	case strings.EqualFold(mediaType, "application/x-www-form-urlencoded"):
		body, err := io.ReadAll(req.Body)
		if err != nil {
			return
		}
		_ = req.Body.Close()
		values, err := url.ParseQuery(string(body))
		if err != nil {
			req.Body = io.NopCloser(bytes.NewReader(body))
			req.ContentLength = int64(len(body))
			return
		}
		decrypted := url.Values{}
		for key, items := range values {
			newKey := decryptValue(enc, key)
			for _, item := range items {
				decrypted.Add(newKey, decryptValue(enc, item))
			}
		}
		encoded := decrypted.Encode()
		req.Body = io.NopCloser(strings.NewReader(encoded))
		req.ContentLength = int64(len(encoded))
	case strings.EqualFold(mediaType, "multipart/form-data"):
		// 仅解密表单字段与文件字段名，文件内容保持原样。
		if err := req.ParseMultipartForm(64 << 20); err != nil {
			return
		}
		if req.MultipartForm == nil {
			return
		}
		decryptedValues := url.Values{}
		for key, items := range req.MultipartForm.Value {
			newKey := decryptValue(enc, key)
			for _, item := range items {
				decryptedValues.Add(newKey, decryptValue(enc, item))
			}
		}
		req.MultipartForm.Value = decryptedValues
		if len(req.MultipartForm.File) > 0 {
			newFiles := map[string][]*multipart.FileHeader{}
			for key, headers := range req.MultipartForm.File {
				newFiles[decryptValue(enc, key)] = headers
			}
			req.MultipartForm.File = newFiles
		}
		req.Form = decryptedValues
	}
}
