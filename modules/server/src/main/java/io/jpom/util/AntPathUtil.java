/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.jpom.util;

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
