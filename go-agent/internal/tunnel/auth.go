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
	"crypto/hmac"
	"crypto/rand"
	"crypto/sha256"
	"encoding/hex"
	"fmt"
	"strconv"
)

// Sign computes the HELLO signature: hex(HMAC-SHA256(key, machineId|ts|nonce)).
func Sign(key, machineID string, ts int64, nonce string) string {
	mac := hmac.New(sha256.New, []byte(key))
	mac.Write([]byte(machineID + "|" + strconv.FormatInt(ts, 10) + "|" + nonce))
	return hex.EncodeToString(mac.Sum(nil))
}

// Nonce returns a random hex string used once per handshake.
func Nonce() string {
	buf := make([]byte, 16)
	if _, err := rand.Read(buf); err != nil {
		return fmt.Sprintf("%d", randFallback())
	}
	return hex.EncodeToString(buf)
}

func randFallback() int64 {
	buf := make([]byte, 8)
	_, _ = rand.Read(buf)
	var value int64
	for _, b := range buf {
		value = value<<8 | int64(b)
	}
	if value < 0 {
		value = -value
	}
	return value
}
