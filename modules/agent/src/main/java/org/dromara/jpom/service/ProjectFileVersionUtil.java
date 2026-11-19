/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * 项目文件版本快照的目录操作工具
 * <p>
 * 抽成静态工具便于单测覆盖「忽略目录、复制、清理、内容指纹」这些会导致用户文件被误删的路径。
 *
 * @author bwcx_jzy
 * @since 2026/11/18
 */
public final class ProjectFileVersionUtil {

    /**
     * 版本目录中不参与快照、切换时也不清理的顶层名称
     */
    public static final List<String> IGNORE_NAMES = Arrays.asList("logs", "temp");
    /**
     * 临时目录标记
     */
    public static final String TEMP_SUFFIX = ".tmp";

    private ProjectFileVersionUtil() {
    }

    /**
     * 是否忽略该文件（顶层 logs/temp 与临时目录）
     *
     * @param file 文件
     * @return true 忽略
     */
    public static boolean isIgnore(File file) {
        String name = FileUtil.getName(file);
        if (StrUtil.contains(name, TEMP_SUFFIX)) {
            return true;
        }
        for (String ignoreName : IGNORE_NAMES) {
            if (StrUtil.equalsIgnoreCase(name, ignoreName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 文件是否位于忽略目录下
     *
     * @param root 根目录
     * @param file 文件
     * @return true 忽略
     */
    public static boolean isInIgnorePath(File root, File file) {
        if (StrUtil.contains(FileUtil.getName(file), TEMP_SUFFIX)) {
            // 临时文件/目录不参与版本内容与指纹（与复制、清理规则保持一致）
            return true;
        }
        String subPath = FileUtil.subPath(root.getAbsolutePath(), file.getAbsolutePath());
        if (StrUtil.isEmpty(subPath)) {
            return false;
        }
        String[] parts = StrUtil.splitToArray(subPath, '/');
        for (String part : parts) {
            for (String ignoreName : IGNORE_NAMES) {
                if (StrUtil.equalsIgnoreCase(part, ignoreName)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 复制目录内容（跳过忽略目录）
     *
     * @param src  源目录
     * @param dest 目标目录
     */
    public static void copyDirContent(File src, File dest) {
        File[] files = src.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (isIgnore(file)) {
                continue;
            }
            if (file.isDirectory()) {
                // Hutool 的目录复制语义：把 src 目录本身复制到 dest 下（得到 dest/<name>），
                // 因此目录要传 dest，文件要传 dest/<name>，否则会多出一层同名目录
                FileUtil.copy(file, dest, true);
            } else {
                FileUtil.copy(file, FileUtil.file(dest, file.getName()), true);
            }
        }
    }

    /**
     * 清空目录（保留忽略目录）
     *
     * @param dir 目录
     */
    public static void clearDir(File dir) {
        FileUtil.mkdir(dir);
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (isIgnore(file)) {
                continue;
            }
            org.dromara.jpom.util.CommandUtil.systemFastDel(file);
        }
    }

    /**
     * 统计参与版本的文件个数
     *
     * @param dir 目录
     * @return int
     */
    public static int countFiles(File dir) {
        int count = 0;
        for (File file : FileUtil.loopFiles(dir)) {
            if (isInIgnorePath(dir, file)) {
                continue;
            }
            count++;
        }
        return count;
    }

    /**
     * 计算目录内容指纹（相对路径 + 大小 + sha1）
     *
     * @param dir 目录
     * @return sha1
     */
    public static String manifest(File dir) {
        List<File> files = FileUtil.loopFiles(dir);
        List<String> lines = new ArrayList<>(files.size());
        for (File file : files) {
            if (isInIgnorePath(dir, file)) {
                continue;
            }
            String subPath = FileUtil.subPath(dir.getAbsolutePath(), file.getAbsolutePath());
            lines.add(subPath + "|" + file.length() + "|" + SecureUtil.sha1(file));
        }
        lines.sort(Comparator.naturalOrder());
        StringBuilder stringBuilder = new StringBuilder();
        for (String line : lines) {
            stringBuilder.append(line).append('\n');
        }
        return SecureUtil.sha1(stringBuilder.toString());
    }
}
