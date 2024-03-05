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
}
