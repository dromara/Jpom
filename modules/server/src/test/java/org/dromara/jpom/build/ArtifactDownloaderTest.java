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

import cn.hutool.crypto.digest.DigestUtil;
import org.dromara.jpom.TestI18nSupport;
import org.dromara.jpom.util.LogRecorder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 构建产物下载器回归测试。
 * <p>
 * 覆盖 2026-11-20 真机事故的三类问题：
 * <ol>
 *     <li>响应被中途截断时，HTTP 客户端不抛异常（只会得到短文件），必须靠 Content-Length 对账发现；</li>
 *     <li>重试后的成功必须真正成功——旧实现用同一个 last 变量记录失败，
 *     导致后面重试成功仍然抛出上一次的异常，构建明明拿到完整产物却显示失败；</li>
 *     <li>截断后的重试应从断点续传，而不是每次从 0 重下。</li>
 * </ol>
 *
 * @author jpom
 * @since 2.11.15
 */
class ArtifactDownloaderTest {

    private static final int PAYLOAD_SIZE = 1000;

    @TempDir
    Path tempDir;

    @BeforeAll
    static void initI18n() {
        // 纯单测没有 Spring 上下文，I18nMessageUtil 需要注入返回 key 的 MessageSource
        TestI18nSupport.init();
    }

    /**
     * 第一次截断（只收到 300 字节）→ 第二次按 Range 续传成功
     */
    @Test
    void truncatedThenResumeFromBreakpoint() throws Exception {
        byte[] payload = payload();
        RecordingFetcher fetcher = new RecordingFetcher(
            (offset, target) -> write(target, payload, 0, 300, false, 200, PAYLOAD_SIZE),
            (offset, target) -> write(target, payload, 300, PAYLOAD_SIZE, true, 206, PAYLOAD_SIZE - 300));
        File target = target();
        try (LogRecorder recorder = LogRecorder.builder().file(new File(tempDir.toFile(), "build.log")).build()) {
            ArtifactDownloader downloader = new ArtifactDownloader(target, PAYLOAD_SIZE, DigestUtil.sha1Hex(payload),
                fetcher, recorder, 5, milliseconds -> {
            });
            long size = downloader.download();
            Assertions.assertEquals(PAYLOAD_SIZE, size);
        }
        Assertions.assertArrayEquals(payload, Files.readAllBytes(target.toPath()));
        // 关键断言：第二次是从 300 续传，不是从 0 重下
        Assertions.assertEquals(Arrays.asList(0L, 300L), fetcher.offsets());
    }

    /**
     * 截断 + 重试成功后必须返回成功（旧实现会抛上一次的旧异常）
     */
    @Test
    void successAfterRetryDoesNotThrowStaleError() throws Exception {
        byte[] payload = payload();
        RecordingFetcher fetcher = new RecordingFetcher(
            (offset, target) -> write(target, payload, 0, 200, false, 200, PAYLOAD_SIZE),
            (offset, target) -> write(target, payload, 0, PAYLOAD_SIZE, false, 200, PAYLOAD_SIZE));
        File target = target();
        try (LogRecorder recorder = LogRecorder.builder().file(new File(tempDir.toFile(), "build.log")).build()) {
            ArtifactDownloader downloader = new ArtifactDownloader(target, PAYLOAD_SIZE, DigestUtil.sha1Hex(payload),
                fetcher, recorder, 5, milliseconds -> {
            });
            Assertions.assertDoesNotThrow(() -> {
                long size = downloader.download();
                Assertions.assertEquals(PAYLOAD_SIZE, size);
            });
        }
    }

    /**
     * 大小对但内容不对（sha1 不匹配）→ 必须清空重下，而不是在错内容上续传
     */
    @Test
    void sha1MismatchRestartsFromZero() throws Exception {
        byte[] payload = payload();
        byte[] wrong = new byte[PAYLOAD_SIZE];
        Arrays.fill(wrong, (byte) 'x');
        RecordingFetcher fetcher = new RecordingFetcher(
            (offset, target) -> write(target, wrong, 0, PAYLOAD_SIZE, false, 200, PAYLOAD_SIZE),
            (offset, target) -> write(target, payload, 0, PAYLOAD_SIZE, false, 200, PAYLOAD_SIZE));
        File target = target();
        try (LogRecorder recorder = LogRecorder.builder().file(new File(tempDir.toFile(), "build.log")).build()) {
            ArtifactDownloader downloader = new ArtifactDownloader(target, PAYLOAD_SIZE, DigestUtil.sha1Hex(payload),
                fetcher, recorder, 5, milliseconds -> {
            });
            Assertions.assertEquals(PAYLOAD_SIZE, downloader.download());
        }
        Assertions.assertArrayEquals(payload, Files.readAllBytes(target.toPath()));
        Assertions.assertEquals(Arrays.asList(0L, 0L), fetcher.offsets());
    }

    /**
     * 老版本节点忽略 Range 返回 200 全量：不能把全量内容追加到断点后面，必须清空重下
     */
    @Test
    void oldAgentWithoutRangeSupportRestarts() throws Exception {
        byte[] payload = payload();
        RecordingFetcher fetcher = new RecordingFetcher(
            (offset, target) -> write(target, payload, 0, 300, false, 200, PAYLOAD_SIZE),
            (offset, target) -> write(target, payload, 0, PAYLOAD_SIZE, true, 200, PAYLOAD_SIZE),
            (offset, target) -> write(target, payload, 0, PAYLOAD_SIZE, false, 200, PAYLOAD_SIZE));
        File target = target();
        try (LogRecorder recorder = LogRecorder.builder().file(new File(tempDir.toFile(), "build.log")).build()) {
            ArtifactDownloader downloader = new ArtifactDownloader(target, PAYLOAD_SIZE, DigestUtil.sha1Hex(payload),
                fetcher, recorder, 5, milliseconds -> {
            });
            Assertions.assertEquals(PAYLOAD_SIZE, downloader.download());
        }
        Assertions.assertArrayEquals(payload, Files.readAllBytes(target.toPath()));
        Assertions.assertEquals(Arrays.asList(0L, 300L, 0L), fetcher.offsets());
    }

    /**
     * 每次都截断：重试耗尽后必须抛「传输被中断」，而不是静默成功
     */
    @Test
    void retryExhaustedThrowsTruncated() throws Exception {
        byte[] payload = payload();
        RecordingFetcher fetcher = new RecordingFetcher(
            (offset, target) -> write(target, payload, 0, 100, false, 200, PAYLOAD_SIZE),
            (offset, target) -> write(target, payload, 0, 100, false, 200, PAYLOAD_SIZE),
            (offset, target) -> write(target, payload, 0, 100, false, 200, PAYLOAD_SIZE));
        File target = target();
        try (LogRecorder recorder = LogRecorder.builder().file(new File(tempDir.toFile(), "build.log")).build()) {
            ArtifactDownloader downloader = new ArtifactDownloader(target, PAYLOAD_SIZE, null,
                fetcher, recorder, 3, milliseconds -> {
            });
            Assertions.assertThrows(ArtifactDownloader.TruncatedException.class, downloader::download);
        }
        Assertions.assertEquals(3, fetcher.attempts());
    }

    /**
     * 复现现场日志：每次都「完整」收到 137/151 字节，只是比声明的产物小 → 必须报大小不一致
     */
    @Test
    void sizeMismatchIsReportedAndRetriedFromZero() throws Exception {
        byte[] payload = payload();
        RecordingFetcher fetcher = new RecordingFetcher(
            (offset, target) -> write(target, payload, 0, 600, false, 200, 600),
            (offset, target) -> write(target, payload, 0, 600, false, 200, 600),
            (offset, target) -> write(target, payload, 0, 600, false, 200, 600));
        File target = target();
        try (LogRecorder recorder = LogRecorder.builder().file(new File(tempDir.toFile(), "build.log")).build()) {
            ArtifactDownloader downloader = new ArtifactDownloader(target, PAYLOAD_SIZE, null,
                fetcher, recorder, 3, milliseconds -> {
            });
            Assertions.assertThrows(ArtifactDownloader.VerifyException.class, downloader::download);
        }
        // 大小不对属于「内容不可信」，每轮都必须从 0 重来
        Assertions.assertEquals(Arrays.asList(0L, 0L, 0L), fetcher.offsets());
    }

    /**
     * 每轮都没有任何进展时不能死循环在同一偏移，必须清空重来
     */
    @Test
    void noProgressRestartsFromZero() throws Exception {
        byte[] payload = payload();
        RecordingFetcher fetcher = new RecordingFetcher(
            (offset, target) -> write(target, payload, 0, 300, false, 200, PAYLOAD_SIZE),
            (offset, target) -> new ArtifactDownloader.Chunk(206, 700, 0),
            (offset, target) -> write(target, payload, 0, PAYLOAD_SIZE, false, 200, PAYLOAD_SIZE));
        File target = target();
        try (LogRecorder recorder = LogRecorder.builder().file(new File(tempDir.toFile(), "build.log")).build()) {
            ArtifactDownloader downloader = new ArtifactDownloader(target, PAYLOAD_SIZE, null,
                fetcher, recorder, 5, milliseconds -> {
            });
            Assertions.assertEquals(PAYLOAD_SIZE, downloader.download());
        }
        Assertions.assertEquals(Arrays.asList(0L, 300L, 0L), fetcher.offsets());
    }

    // ---------------------------------------------------------------- 测试夹具

    private File target() {
        return new File(tempDir.toFile(), "artifact.zip");
    }

    private static byte[] payload() {
        byte[] payload = new byte[PAYLOAD_SIZE];
        for (int i = 0; i < payload.length; i++) {
            payload[i] = (byte) (i * 31 + 7);
        }
        return payload;
    }

    /**
     * 模拟「一次拉取」：从 payload 的 from..to 写入目标文件，返回本次结果
     */
    private static ArtifactDownloader.Chunk write(File target, byte[] payload, int from, int to, boolean append,
                                                  int status, long declaredLength) throws Exception {
        StandardOpenOption[] options = append
            ? new StandardOpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE}
            : new StandardOpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE};
        byte[] slice = Arrays.copyOfRange(payload, from, to);
        Files.write(target.toPath(), slice, options);
        return new ArtifactDownloader.Chunk(status, declaredLength, slice.length);
    }

    /**
     * 按脚本依次返回结果的假拉取器，同时记录每次请求的断点偏移
     */
    private static final class RecordingFetcher implements ArtifactDownloader.ChunkFetcher {

        private final List<ArtifactDownloader.ChunkFetcher> steps;
        private final List<Long> offsets = new ArrayList<>();
        private int index;

        private RecordingFetcher(ArtifactDownloader.ChunkFetcher... steps) {
            this.steps = Arrays.asList(steps);
        }

        @Override
        public ArtifactDownloader.Chunk fetch(long offset, File target) throws Exception {
            this.offsets.add(offset);
            ArtifactDownloader.ChunkFetcher step = this.steps.get(Math.min(this.index, this.steps.size() - 1));
            this.index++;
            return step.fetch(offset, target);
        }

        private List<Long> offsets() {
            return this.offsets;
        }

        private int attempts() {
            return this.index;
        }
    }
}
