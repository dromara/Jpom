/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package project

import (
	"fmt"
	"os"
	"path/filepath"
	"runtime"
	"sort"
	"strings"

	"jpom.io/go-agent/internal/proc"
)

// pathSeparator 返回 classpath 分隔符（FileUtil.PATH_SEPARATOR）。
func pathSeparator() string {
	if runtime.GOOS == "windows" {
		return ";"
	}
	return ":"
}

// javaPath 返回 java/javaw 字面量（等价 Java getRunJavaPath）。
func javaPath(windows bool) string {
	if windows {
		return "javaw"
	}
	return "java"
}

// resolveJavaPath 解析可执行的 java 命令：PATH 命中返回字面量，否则回退 JAVA_HOME/常见路径。
func resolveJavaPath(windows bool) (string, error) {
	return proc.JavaCommand(windows)
}

// jpomPidTag 生成 jpom 进程标识（等价 JvmUtil.getJpomPidTag）。
func jpomPidTag(id, lib string) string {
	return fmt.Sprintf("-DJpom.application=%s -DJpom.basedir=%s", id, lib)
}

// listJars 列出目录下的 jar/war 文件（等价 AbstractProjectCommander.listJars）。
func listJars(runMode, dir string) []string {
	entries, err := os.ReadDir(dir)
	if err != nil {
		return nil
	}
	files := make([]string, 0, len(entries))
	for _, entry := range entries {
		if entry.IsDir() {
			continue
		}
		name := entry.Name()
		switch runMode {
		case RunModeClassPath, RunModeJar, RunModeJavaExtDirsCp:
			if strings.HasSuffix(strings.ToLower(name), ".jar") {
				files = append(files, filepath.Join(dir, name))
			}
		case RunModeJarWar:
			if strings.HasSuffix(strings.ToLower(name), ".war") {
				files = append(files, filepath.Join(dir, name))
			}
		}
	}
	sort.Strings(files)
	return files
}

// classPathLib 拼接 java 的 classpath/jar 参数（等价 AbstractProjectCommander.getClassPathLib）。
func classPathLib(original map[string]any, lib string) string {
	runMode := GetString(original, "runMode")
	files := listJars(runMode, lib)
	if len(files) == 0 {
		return ""
	}
	var builder strings.Builder
	count := len(files)
	switch runMode {
	case RunModeClassPath:
		builder.WriteString("-classpath ")
	case RunModeJar, RunModeJarWar:
		builder.WriteString("-jar ")
		count = 1
	case RunModeJavaExtDirsCp:
		builder.WriteString("-Djava.ext.dirs=")
		ext := GetString(original, "javaExtDirsCp")
		parts := make([]string, 0, 2)
		for _, part := range strings.Split(ext, ":") {
			part = strings.TrimSpace(part)
			if part != "" {
				parts = append(parts, part)
			}
		}
		if len(parts) == 0 {
			builder.WriteString(". -cp ")
		} else {
			builder.WriteString(parts[0])
			builder.WriteString(" -cp ")
			if len(parts) > 1 {
				builder.WriteString(parts[1])
				builder.WriteString(pathSeparator())
			}
		}
	default:
		return ""
	}
	for i := 0; i < count; i++ {
		builder.WriteString(files[i])
		if i != count-1 {
			builder.WriteString(pathSeparator())
		}
	}
	return builder.String()
}

// BuildRunCommand 生成项目实际执行的启动命令（等价 ProjectCommander.buildRunCommand）。
//
// 解析异常（如软链项目不存在）会被吞掉并返回空串，供 WS 等非严格场景使用；
// HTTP 端点应使用 RunCommandE 以便按 Java 语义返回 405。
func (r *Runtime) BuildRunCommand(info map[string]any) string {
	command, _ := r.RunCommandE(info)
	return command
}

// RunCommandE 生成启动命令并返回解析错误（条件不足时返回空串 + nil，与 Java 一致）。
func (r *Runtime) RunCommandE(info map[string]any) (string, error) {
	original, err := r.Manager.ResolveModel(info)
	if err != nil {
		return "", err
	}
	return r.buildRunCommand(info, original), nil
}

// buildRunCommand 生成启动命令；条件不足时返回空字符串。
func (r *Runtime) buildRunCommand(info, original map[string]any) string {
	lib, err := r.Manager.ResolveLibPath(original)
	if err != nil {
		return ""
	}
	classPath := classPathLib(original, lib)
	if strings.TrimSpace(classPath) == "" {
		return ""
	}
	tag := GetString(info, "id")
	logFile, err := r.Manager.TouchLogFile(info)
	if err != nil {
		return ""
	}
	absLog, err := filepath.Abs(logFile)
	if err != nil {
		absLog = logFile
	}
	jvm := GetString(info, "jvm")
	mainClass := GetString(original, "mainClass")
	args := GetString(info, "args")
	pids := jpomPidTag(tag, lib)
	javaBin, err := resolveJavaPath(runtime.GOOS == "windows")
	if err != nil {
		// 命令展示仍与 Java 一致；启动时 checkStart 会返回明确错误
		javaBin = javaPath(runtime.GOOS == "windows")
	}
	if runtime.GOOS == "windows" {
		// Windows 的启动命令不合并 stderr（与 Java 实现一致）
		return fmt.Sprintf("%s %s %s %s %s %s >> %s &",
			javaBin, jvm, pids, classPath, mainClass, args, absLog)
	}
	return fmt.Sprintf("nohup %s %s %s %s %s %s >> %s 2>&1 &",
		javaBin, jvm, pids, classPath, mainClass, args, absLog)
}
