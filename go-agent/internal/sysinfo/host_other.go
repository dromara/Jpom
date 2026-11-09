//go:build !linux

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

import "fmt"

// 非 Linux 平台暂未实现系统信息采集（保留跨平台结构，后续可按 Oshi 语义补齐）。
var errUnsupportedOS = fmt.Errorf("当前操作系统暂不支持该监控项")

func readCPUTicks() (CPUTicks, error)                       { return CPUTicks{}, errUnsupportedOS }
func cpuCount() int                                         { return 0 }
func cpuModel() string                                      { return "unknown" }
func cpuIdentifierName() string                             { return "unknown" }
func readMemory() (memoryStat, error)                       { return memoryStat{}, errUnsupportedOS }
func readUptime() (int64, error)                            { return 0, errUnsupportedOS }
func readHostName() (string, error)                         { return "", errUnsupportedOS }
func osVersion() string                                     { return "" }
func hardwareVersion() string                               { return "" }
func localIPv4s() []string                                  { return []string{} }
func loadAverage() []float64                                { return []float64{0, 0, 0} }
func listFileStores() ([]fileStore, error)                  { return nil, errUnsupportedOS }
func listDiskStores() ([]diskStore, error)                  { return nil, errUnsupportedOS }
func listNetworkInterfaces() ([]netIface, error)            { return nil, errUnsupportedOS }
func readNetStats(*NetworkFilter, bool) (netStatSum, error) { return netStatSum{}, errUnsupportedOS }
func listProcesses() ([]rawProcess, error)                  { return nil, errUnsupportedOS }
func killPID(pid int) error                                 { return errUnsupportedOS }
func listeningPorts(pid int) string                         { return "-" }
