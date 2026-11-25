/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.pipeline.engine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 运行日志按窗口读取
 * <p>
 * 前端每 2 秒按「下一行」翻页拉日志。旧实现每次 {@code readLines} 整个文件：长日志（几十 MB）下
 * 每次轮询都整文件读+整文件进内存，既慢又吃堆。这里只读到窗口末尾为止（多读一行判断是否还有更多），
 * 内存只保留一页。
 * </p>
 * <p>
 * {@code totalLines} 保留字段但不再统计（返回 -1）：统计总行数必须整文件扫描，与优化目标冲突，
 * 且前端只用 {@code dataLines} 与 {@code hasMore} 两个字段。
 * </p>
 *
 * @author jpom
 * @since 2.11.16
 */
public final class PipelineLogReader {

    private PipelineLogReader() {
    }

    /**
     * 读取结果
     */
    public static final class Window {
        private final List<String> lines;
        private final boolean hasMore;
        private final int totalLines;

        Window(List<String> lines, boolean hasMore, int totalLines) {
            this.lines = lines;
            this.hasMore = hasMore;
            this.totalLines = totalLines;
        }

        public List<String> getLines() {
            return lines;
        }

        public boolean isHasMore() {
            return hasMore;
        }

        /**
         * @return 总行数；-1 表示本实现不统计
         */
        public int getTotalLines() {
            return totalLines;
        }
    }

    /**
     * 从 {@code startLine}（1 开始）读取至多 {@code pageSize} 行
     *
     * @param file      日志文件
     * @param startLine 起始行号（&lt;1 按 1 处理）
     * @param pageSize  每页行数（&lt;1 按 1 处理）
     * @return 窗口；文件不存在返回空窗口
     * @throws IOException 读取失败
     */
    public static Window read(File file, int startLine, int pageSize) throws IOException {
        int start = Math.max(startLine, 1);
        int size = Math.max(pageSize, 1);
        if (file == null || !file.isFile()) {
            return new Window(Collections.<String>emptyList(), false, -1);
        }
        List<String> lines = new ArrayList<>(size);
        boolean hasMore = false;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            // 跳到起始行：只丢弃不缓存
            for (int skipped = 0; skipped < start - 1; skipped++) {
                if (reader.readLine() == null) {
                    return new Window(lines, false, -1);
                }
            }
            String line;
            while ((line = reader.readLine()) != null) {
                if (lines.size() < size) {
                    lines.add(line);
                } else {
                    hasMore = true;
                    break;
                }
            }
        }
        return new Window(lines, hasMore, -1);
    }
}
