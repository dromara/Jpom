/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.build;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.util.LogRecorder;

import java.io.File;

/**
 * 构建产物下载器：分片续传 + 字节对账 + 校验 + 重试。
 * <p>
 * 拆出来的原因（2026-11-20 真机事故）：
 * <ul>
 *     <li>HTTP 客户端在对端提前关闭连接时<b>不会抛异常</b>，只会把短文件当作正常 EOF，
 *     只有把「响应声明的 Content-Length」和「实际落盘字节」对上账才能判定是传输被中断；</li>
 *     <li>原来的重试循环用同一个 {@code last} 变量既记失败又决定是否抛错，导致
 *     <b>后面某次重试成功后仍然抛出上一次的旧异常</b>，构建明明拿到了完整产物却显示失败；</li>
 *     <li>每次重试都从 0 重下，大产物在抖动链路上几乎不可能成功。</li>
 * </ul>
 * 传输实现（正向 HTTP / 反向隧道）通过 {@link ChunkFetcher} 注入，便于单测覆盖截断、续传、校验失败等分支。
 *
 * @author jpom
 * @since 2.11.15
 */
@Slf4j
class ArtifactDownloader {

    /**
     * 重试间隔上限（秒）
     */
    private static final long MAX_BACKOFF_SECONDS = 30;

    private final File target;
    private final long expectSize;
    private final String expectSha1;
    private final ChunkFetcher fetcher;
    private final LogRecorder logRecorder;
    private final int maxAttempts;
    private final Sleeper sleeper;

    ArtifactDownloader(File target, long expectSize, String expectSha1, ChunkFetcher fetcher,
                       LogRecorder logRecorder, int maxAttempts) {
        this(target, expectSize, expectSha1, fetcher, logRecorder, maxAttempts, milliseconds -> Thread.sleep(milliseconds));
    }

    ArtifactDownloader(File target, long expectSize, String expectSha1, ChunkFetcher fetcher,
                       LogRecorder logRecorder, int maxAttempts, Sleeper sleeper) {
        this.target = target;
        this.expectSize = expectSize;
        this.expectSha1 = expectSha1;
        this.fetcher = fetcher;
        this.logRecorder = logRecorder;
        this.maxAttempts = Math.max(maxAttempts, 1);
        this.sleeper = sleeper;
    }

    /**
     * 下载并校验产物
     *
     * @return 落盘产物大小
     * @throws Exception 重试耗尽后抛出最后一次失败原因
     */
    long download() throws Exception {
        FileUtil.mkParentDirs(this.target);
        FileUtil.del(this.target);
        Exception last = null;
        // 上一轮使用的断点：用于识别「连续两轮都停在同一个偏移」的死循环
        long previousOffset = -1;
        for (int attempt = 1; attempt <= this.maxAttempts; attempt++) {
            long offset = FileUtil.size(this.target);
            if (offset > 0 && offset == previousOffset) {
                // 上一轮没有任何进展（例如节点侧 Range 无法满足）：清空重来，避免卡死在同一个偏移
                this.logRecorder.system(I18nMessageUtil.get("i18n.build_node_download_no_progress.c4e8"), offset, this.expectSize);
                FileUtil.del(this.target);
                offset = 0;
            }
            previousOffset = offset;
            long begin = System.currentTimeMillis();
            try {
                Chunk chunk = this.fetcher.fetch(offset, this.target);
                long elapsed = System.currentTimeMillis() - begin;
                long total = FileUtil.size(this.target);
                if (chunk.getStatus() == 200 && offset > 0) {
                    // 节点不支持 Range：这一片是全量内容，被追加到旧字节后面，只能清空重来
                    FileUtil.del(this.target);
                    throw new IllegalStateException(I18nMessageUtil.get("i18n.build_node_download_resume_unsupported.d9a2"));
                }
                this.logRecorder.system(I18nMessageUtil.get("i18n.build_node_download_attempt.a41f"),
                    attempt, offset, chunk.getReceived(), total, this.expectSize, elapsed);
                if (chunk.getDeclaredLength() >= 0 && chunk.getReceived() < chunk.getDeclaredLength()) {
                    throw new TruncatedException(chunk.getDeclaredLength(), chunk.getReceived(), total, this.expectSize);
                }
                this.verify(total);
                this.logRecorder.system(I18nMessageUtil.get("i18n.build_node_download_success.e3f5"), attempt, total);
                return total;
            } catch (Exception e) {
                last = e;
                if (!resumable(e)) {
                    FileUtil.del(this.target);
                    previousOffset = -1;
                }
                this.logRecorder.systemError(I18nMessageUtil.get("i18n.build_node_download_retry.6d84"),
                    attempt, this.maxAttempts, e.getMessage());
                if (attempt < this.maxAttempts) {
                    this.sleep(attempt);
                }
            }
        }
        throw last;
    }

    /**
     * 是否为可续传的失败（已落盘字节可信，可以从断点继续）
     *
     * @param e 失败原因
     * @return true 保留已落盘内容
     */
    private static boolean resumable(Exception e) {
        // 校验不过说明内容本身不对（不是短读），留着也拼不回来
        return !(e instanceof VerifyException);
    }

    private void verify(long total) {
        if (this.expectSize > 0 && total != this.expectSize) {
            throw new VerifyException(StrUtil.format(I18nMessageUtil.get("i18n.build_node_result_size_mismatch.2f68"),
                this.expectSize, total));
        }
        if (StrUtil.isNotEmpty(this.expectSha1)) {
            String sha1 = SecureUtil.sha1(this.target);
            if (!StrUtil.equalsIgnoreCase(sha1, this.expectSha1)) {
                throw new VerifyException(StrUtil.format(I18nMessageUtil.get("i18n.build_node_result_sha1_mismatch.8d27"),
                    this.expectSha1, sha1));
            }
        }
    }

    private void sleep(int attempt) {
        long seconds = Math.min((long) attempt * 5, MAX_BACKOFF_SECONDS);
        try {
            this.sleeper.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(I18nMessageUtil.get("i18n.system_interruption.e37c"), e);
        }
    }

    /**
     * 拉取一片产物
     */
    interface ChunkFetcher {
        /**
         * 从指定偏移拉取一片并写入文件（偏移大于 0 时追加）
         *
         * @param offset 已落盘字节数（断点）
         * @param target 目标文件
         * @return 本次拉取结果
         * @throws Exception 传输失败
         */
        Chunk fetch(long offset, File target) throws Exception;
    }

    /**
     * 单次拉取结果
     */
    static class Chunk {
        private final int status;
        private final long declaredLength;
        private final long received;

        Chunk(int status, long declaredLength, long received) {
            this.status = status;
            this.declaredLength = declaredLength;
            this.received = received;
        }

        int getStatus() {
            return status;
        }

        /**
         * 响应声明的长度（-1 未知）
         *
         * @return 字节数
         */
        long getDeclaredLength() {
            return declaredLength;
        }

        /**
         * 本次实际写入字节数
         *
         * @return 字节数
         */
        long getReceived() {
            return received;
        }
    }

    /**
     * 响应被中途截断（对端少发了字节）
     */
    static class TruncatedException extends IllegalStateException {
        TruncatedException(long declaredLength, long received, long total, long expectSize) {
            super(StrUtil.format(I18nMessageUtil.get("i18n.build_node_download_truncated.f7c3"),
                declaredLength, received, total, expectSize));
        }
    }

    /**
     * 产物校验不通过
     */
    static class VerifyException extends IllegalStateException {
        VerifyException(String message) {
            super(message);
        }
    }

    /**
     * 重试等待
     */
    interface Sleeper {
        /**
         * 等待
         *
         * @param milliseconds 毫秒
         * @throws InterruptedException 被中断
         */
        void sleep(long milliseconds) throws InterruptedException;
    }
}
