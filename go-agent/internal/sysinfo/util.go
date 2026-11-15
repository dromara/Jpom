/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package sysinfo

import "strings"

// usrHz 是 Linux 上 /proc 中 tick 的时钟频率（getconf CLK_TCK，绝大多数发行版为 100）。
// Oshi 会把 jiffies 换算成毫秒，这里按同样的口径换算。
const usrHz = 100

// osHI 的 tick 单位是毫秒：jiffies * (1000 / usrHz)。
func ticksToMillis(jiffies uint64) uint64 { return jiffies * (1000 / usrHz) }

func equalFold(a, b string) bool { return strings.EqualFold(a, b) }

// netStatSum 是网络累计流量快照。
type netStatSum struct {
	BytesRecv   uint64
	BytesSent   uint64
	PacketsRecv uint64
	PacketsSent uint64
	IfsNames    string
}
