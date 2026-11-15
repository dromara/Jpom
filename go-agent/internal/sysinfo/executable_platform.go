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

import (
	"encoding/binary"
	"errors"
	"fmt"
	"io"
	"os"
)

// 可执行文件头校验（CONTRACT §3.3）。
//
// 只校验 OS magic 不足以阻止跨架构升级包：ELF e_machine/PE Machine/Mach-O
// cputype 都必须与目标平台一致，否则 405 且不替换当前二进制。
const (
	elfMachineAMD64 uint16 = 0x3e
	elfMachineARM64 uint16 = 0xb7

	peMachineAMD64 uint16 = 0x8664
	peMachineARM64 uint16 = 0xaa64

	machCPUTypeAMD64 uint32 = 0x01000007
	machCPUTypeARM64 uint32 = 0x0100000c

	// Mach-O magic（按大端读取前 4 字节后的值）。
	machMagic32  uint32 = 0xfeedface
	machMagic64  uint32 = 0xfeedfacf
	machCigam32  uint32 = 0xcefaedfe
	machCigam64  uint32 = 0xcffaedfe
	machFatMagic uint32 = 0xcafebabe
	machFatCigam uint32 = 0xbebafeca
)

var errNotExecutable = errors.New("not an executable file")

// verifyExecutable 校验文件头是否与当前运行平台匹配。
func verifyExecutable(path string) error {
	return verifyExecutableFor(path, currentPlatform())
}

// verifyExecutableFor 校验 path 是否为 target 平台的可执行文件。
// 校验顺序：OS magic -> 架构 machine。失败返回 405 ParamError。
func verifyExecutableFor(path string, target targetPlatform) error {
	switch target.GOOS {
	case "windows":
		machine, err := readPEMachine(path)
		if err != nil {
			return paramErr(405, "i18n.go_upgrade_invalid_windows_executable")
		}
		return checkExecutableMachine(target, uint64(machine), uint64(peMachineAMD64), uint64(peMachineARM64))
	case "darwin":
		cpuTypes, err := readMachOCPUTypes(path)
		if err != nil {
			return paramErr(405, "i18n.go_upgrade_invalid_macos_executable")
		}
		return checkMachOCPUType(target, cpuTypes)
	default:
		// Linux 及使用 ELF 的其他类 Unix 平台。
		machine, err := readELFMachine(path)
		if err != nil {
			return paramErr(405, "i18n.go_upgrade_invalid_linux_executable")
		}
		return checkExecutableMachine(target, uint64(machine), uint64(elfMachineAMD64), uint64(elfMachineARM64))
	}
}

// checkExecutableMachine 比较 ELF e_machine / PE Machine 与目标架构。
func checkExecutableMachine(target targetPlatform, machine, amd64, arm64 uint64) error {
	var expected uint64
	switch target.GOARCH {
	case "amd64":
		expected = amd64
	case "arm64":
		expected = arm64
	default:
		return paramErr(405, "i18n.go_upgrade_platform_mismatch", fmt.Sprintf("%s (unsupported architecture)", target.String()))
	}
	if machine != expected {
		return paramErr(405, "i18n.go_upgrade_platform_mismatch", fmt.Sprintf("%s (executable machine=0x%x, expected 0x%x)", target.String(), machine, expected))
	}
	return nil
}

// checkMachOCPUType 判断 Mach-O 的 cputype（或 fat 包内任一 cputype）是否匹配目标架构。
func checkMachOCPUType(target targetPlatform, cpuTypes []uint32) error {
	var expected uint32
	switch target.GOARCH {
	case "amd64":
		expected = machCPUTypeAMD64
	case "arm64":
		expected = machCPUTypeARM64
	default:
		return paramErr(405, "i18n.go_upgrade_platform_mismatch", fmt.Sprintf("%s (unsupported architecture)", target.String()))
	}
	for _, cpuType := range cpuTypes {
		if cpuType == expected {
			return nil
		}
	}
	return paramErr(405, "i18n.go_upgrade_platform_mismatch", fmt.Sprintf("%s (executable cputype=0x%x, expected 0x%x)", target.String(), cpuTypes, expected))
}

// readELFMachine 读取 ELF e_machine（offset 18，按 EI_DATA 指定字节序）。
func readELFMachine(path string) (uint16, error) {
	file, err := os.Open(path)
	if err != nil {
		return 0, err
	}
	defer func() { _ = file.Close() }()
	header := make([]byte, 64)
	if _, err := io.ReadFull(file, header); err != nil {
		return 0, errNotExecutable
	}
	if header[0] != 0x7f || header[1] != 'E' || header[2] != 'L' || header[3] != 'F' {
		return 0, errNotExecutable
	}
	var order binary.ByteOrder
	switch header[5] {
	case 1:
		order = binary.LittleEndian
	case 2:
		order = binary.BigEndian
	default:
		return 0, errNotExecutable
	}
	return order.Uint16(header[18:20]), nil
}

// readPEMachine 读取 PE COFF header 的 Machine 字段（e_lfanew 指向 "PE\0\0" 后偏移 4）。
func readPEMachine(path string) (uint16, error) {
	file, err := os.Open(path)
	if err != nil {
		return 0, err
	}
	defer func() { _ = file.Close() }()
	dosHeader := make([]byte, 64)
	if _, err := io.ReadFull(file, dosHeader); err != nil {
		return 0, errNotExecutable
	}
	if dosHeader[0] != 'M' || dosHeader[1] != 'Z' {
		return 0, errNotExecutable
	}
	offset := int64(binary.LittleEndian.Uint32(dosHeader[0x3c:0x40]))
	if offset < 64 || offset > 1<<20 {
		return 0, errNotExecutable
	}
	signature := make([]byte, 6)
	if _, err := file.ReadAt(signature, offset); err != nil {
		return 0, errNotExecutable
	}
	if signature[0] != 'P' || signature[1] != 'E' || signature[2] != 0 || signature[3] != 0 {
		return 0, errNotExecutable
	}
	return binary.LittleEndian.Uint16(signature[4:6]), nil
}

// readMachOCPUTypes 读取 Mach-O 的 cputype；fat（universal）包返回全部架构。
func readMachOCPUTypes(path string) ([]uint32, error) {
	file, err := os.Open(path)
	if err != nil {
		return nil, err
	}
	defer func() { _ = file.Close() }()
	header := make([]byte, 8)
	if _, err := io.ReadFull(file, header); err != nil {
		return nil, errNotExecutable
	}
	switch magic := binary.BigEndian.Uint32(header[0:4]); magic {
	case machMagic32, machMagic64:
		return []uint32{binary.BigEndian.Uint32(header[4:8])}, nil
	case machCigam32, machCigam64:
		return []uint32{binary.LittleEndian.Uint32(header[4:8])}, nil
	case machFatMagic:
		return readFatMachOCPUTypes(file, binary.BigEndian)
	case machFatCigam:
		return readFatMachOCPUTypes(file, binary.LittleEndian)
	default:
		return nil, errNotExecutable
	}
}

// readFatMachOCPUTypes 解析 fat/universal 二进制头部（fat_arch 20 字节）。
func readFatMachOCPUTypes(file *os.File, order binary.ByteOrder) ([]uint32, error) {
	header := make([]byte, 8)
	if _, err := file.ReadAt(header, 0); err != nil {
		return nil, errNotExecutable
	}
	archCount := order.Uint32(header[4:8])
	if archCount == 0 || archCount > 64 {
		return nil, errNotExecutable
	}
	entries := make([]byte, int(archCount)*20)
	if _, err := file.ReadAt(entries, 8); err != nil {
		return nil, errNotExecutable
	}
	cpuTypes := make([]uint32, 0, archCount)
	for index := 0; index < int(archCount); index++ {
		cpuTypes = append(cpuTypes, order.Uint32(entries[index*20:index*20+4]))
	}
	return cpuTypes, nil
}
