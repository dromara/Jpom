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

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.util.AntPathMatcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * @author bwcx_jzy
 * @since 2023/2/9
 */
public class AntPathUtil {


    public static final AntPathMatcher ANT_PATH_MATCHER = new AntPathMatcher();

    public static List<String> antPathMatcher(File rootFile, String match) {
        // 格式化，并处理 Slip 问题
        String matchStr = FileUtil.normalize(StrUtil.SLASH + match);
        List<String> paths = new ArrayList<>();
        //
        FileUtil.walkFiles(rootFile.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                return this.test(file);
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes exc) throws IOException {
                return this.test(dir);
            }

            private FileVisitResult test(Path path) {
                String subPath = FileUtil.subPath(FileUtil.getAbsolutePath(rootFile), path.toFile());
                subPath = FileUtil.normalize(StrUtil.SLASH + subPath);
                if (ANT_PATH_MATCHER.match(matchStr, subPath)) {
                    paths.add(subPath);
                    //return FileVisitResult.TERMINATE;
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return paths;
    }

    /**
     * 判断路径是否匹配任一Ant规则
     * <p>
     * 支持直接配置目录路径（如 /modules/server），会自动尝试目录下的所有文件匹配。
     * 即配置 /modules/server 等价于同时匹配 /modules/server 和 /modules/server/**
     *
     * @param path  以/开头的文件路径
     * @param rules 逗号分隔的Ant规则，null或空返回false
     * @return true 如果匹配任一规则
     */
    public static boolean matchAny(String path, String rules) {
        if (StrUtil.isEmpty(rules)) {
            return false;
        }
        List<String> ruleList = StrUtil.splitTrim(rules, StrUtil.COMMA);
        for (String rule : ruleList) {
            String normalizedRule = FileUtil.normalize(StrUtil.SLASH + rule);
            if (ANT_PATH_MATCHER.match(normalizedRule, path)) {
                return true;
            }
            // 如果规则不含通配符，自动尝试作为目录匹配（追加 /**）
            // 这样用户配置 /modules/server 就能匹配该目录下所有文件
            if (!StrUtil.containsAny(normalizedRule, "*", "?", "{")) {
                String dirRule = normalizedRule + "/**";
                if (ANT_PATH_MATCHER.match(dirRule, path)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 差异路径过滤：include匹配 + exclude排除
     * <p>
     * includePath为空 → 通过（向后兼容）
     * includePath非空 → 至少匹配一个 → 再检查excludePath排除
     *
     * @param path        变更文件的仓库相对路径
     * @param includePath 包含规则（逗号分隔），null或空表示不过滤
     * @param excludePath 排除规则（逗号分隔），null或空表示不排除
     * @return true 表示该文件变更有效（触发构建）
     */
    public static boolean matchDiffPath(String path, String includePath, String excludePath) {
        String normalizedPath = FileUtil.normalize(StrUtil.SLASH + path);
        if (StrUtil.isNotEmpty(includePath) && !matchAny(normalizedPath, includePath)) {
            return false;
        }
        if (StrUtil.isNotEmpty(excludePath) && matchAny(normalizedPath, excludePath)) {
            return false;
        }
        return true;
    }
}
