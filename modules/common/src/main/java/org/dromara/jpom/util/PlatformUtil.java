/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.util;

import cn.hutool.core.util.StrUtil;

import java.util.Locale;

/**
 * 运行平台归一化（os/arch）。
 * <p>
 * 服务端（升级包按平台选择）、Java 插件端（manifest 上报 platform/arch）与 Go 插件端
 * （{@code runtime.GOOS/GOARCH} 原值）共用同一套口径：os 归一到 linux/windows/darwin，
 * arch 归一到 amd64/arm64/386，避免同一台机器在两端算出不同的平台字符串。
 *
 * @author jpom
 * @since 2026/11/20
 */
public final class PlatformUtil {

    private PlatformUtil() {
    }

    /**
     * 规范化平台字符串（os/arch，忽略大小写与首尾空白）
     *
     * @param platform 平台，例如 linux/amd64、Linux/ARM64
     * @return 规范化平台（小写、arch 别名归一），非法格式返回 null
     */
    public static String normalizePlatform(String platform) {
        if (StrUtil.isBlank(platform)) {
            return null;
        }
        String value = StrUtil.trim(platform).toLowerCase(Locale.ROOT);
        int index = value.indexOf('/');
        if (index <= 0 || index >= value.length() - 1) {
            return null;
        }
        String os = normalizeOs(value.substring(0, index));
        String arch = normalizeArch(value.substring(index + 1));
        if (StrUtil.isBlank(os) || StrUtil.isBlank(arch)) {
            return null;
        }
        return os + "/" + arch;
    }

    /**
     * 由系统名与架构拼出规范化平台
     *
     * @param osName 系统名（兼容 Java 插件端 osName 与 Go runtime.GOOS）
     * @param arch   架构
     * @return 规范化平台，无法识别返回 null
     */
    public static String of(String osName, String arch) {
        String os = normalizeOs(osName);
        String normalizeArch = normalizeArch(arch);
        if (StrUtil.isBlank(os) || StrUtil.isBlank(normalizeArch)) {
            return null;
        }
        return os + "/" + normalizeArch;
    }

    /**
     * 规范化系统名称（兼容 Java 插件端 osName 与 Go runtime.GOOS）
     *
     * @param osName 系统名称
     * @return 规范化系统名（linux/windows/darwin 等），无法识别返回 null
     */
    public static String normalizeOs(String osName) {
        if (StrUtil.isBlank(osName)) {
            return null;
        }
        String os = StrUtil.trim(osName).toLowerCase(Locale.ROOT);
        if ("linux".equals(os)) {
            return "linux";
        }
        if ("windows".equals(os) || "win".equals(os) || "win32".equals(os)) {
            return "windows";
        }
        if ("darwin".equals(os) || "mac".equals(os) || "macos".equals(os) || "mac os x".equals(os) || "osx".equals(os)) {
            return "darwin";
        }
        // 其他平台保留小写原值（仍做精确比较，避免把未知系统误判为不支持）
        return os.matches("[a-z0-9]+") ? os : null;
    }

    /**
     * 规范化 CPU 架构（兼容 Go runtime.GOARCH 与常见别名）
     *
     * @param arch 架构
     * @return 规范化架构（amd64/arm64 等），无法识别返回 null
     */
    public static String normalizeArch(String arch) {
        if (StrUtil.isBlank(arch)) {
            return null;
        }
        String value = StrUtil.trim(arch).toLowerCase(Locale.ROOT);
        if ("x86_64".equals(value) || "x64".equals(value) || "amd64".equals(value)) {
            return "amd64";
        }
        if ("aarch64".equals(value) || "arm64v8".equals(value) || "arm64".equals(value)) {
            return "arm64";
        }
        if ("x86".equals(value) || "i386".equals(value) || "i686".equals(value) || "386".equals(value)) {
            return "386";
        }
        return value.matches("[a-z0-9_]+") ? value : null;
    }
}
