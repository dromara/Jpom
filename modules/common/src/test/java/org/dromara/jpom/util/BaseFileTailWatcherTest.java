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

import cn.hutool.extra.spring.SpringUtil;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.StaticMessageSource;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * {@link BaseFileTailWatcher} 行为级回归测试。
 *
 * <p>覆盖 2.11.13.1 之后插件端 “项目列表/控制台来回打开关闭导致 agent 堆内存持续增长”
 * 暴露出的会话生命周期问题：会话集合非线程安全、watcher 关闭与新增会话竞态、
 * 关闭非幂等导致 Tailer 线程泄漏。
 *
 * @author bwcx_jzy
 */
public class BaseFileTailWatcherTest {

    /**
     * BaseFileTailWatcher 内部会记录 i18n 日志，这里初始化一个最小的 Spring 上下文，
     * 避免单测环境缺少 MessageSource 导致 I18nMessageUtil 静态初始化失败。
     */
    @BeforeClass
    public static void initI18nContext() {
        if (SpringUtil.getApplicationContext() != null) {
            return;
        }
        GenericApplicationContext context = new GenericApplicationContext();
        // 默认 messageSource 找不到 key 会抛 NoSuchMessageException，这里用 code 兜底
        StaticMessageSource messageSource = new StaticMessageSource();
        messageSource.setUseCodeAsDefaultMessage(true);
        context.registerBean("messageSource", MessageSource.class, () -> messageSource);
        context.refresh();
        new SpringUtil().setApplicationContext(context);
    }

    /**
     * 会话唯一标识通过反射 {@code getId()} 获取，与真实 WebSocket 会话保持一致
     */
    private static class FakeSession implements AutoCloseable {
        private final String id;
        private final boolean failSend;
        private final AtomicInteger sendCount = new AtomicInteger();
        private volatile boolean closed;

        private FakeSession(String id) {
            this(id, false);
        }

        private FakeSession(String id, boolean failSend) {
            this.id = id;
            this.failSend = failSend;
        }

        public String getId() {
            return id;
        }

        public boolean isClosed() {
            return closed;
        }

        public int getSendCount() {
            return sendCount.get();
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    private static class TestWatcher extends BaseFileTailWatcher<FakeSession> {
        private final List<String> sent = new ArrayList<>();

        private TestWatcher(File logFile) {
            super(logFile, StandardCharsets.UTF_8);
        }

        @Override
        protected boolean send(FakeSession session, String msg) throws IOException {
            if (session.failSend) {
                throw new IOException("mock send failure");
            }
            session.sendCount.incrementAndGet();
            synchronized (sent) {
                sent.add(msg);
            }
            return true;
        }

        private boolean addSession(FakeSession session) throws IOException {
            return add(session, "test.log");
        }

        private void closeIfEmptyForTest() {
            closeIfEmpty();
        }

        private void closeForTest() {
            close();
        }

        private int sessionCount() {
            return socketSessions.size();
        }

        private boolean empty() {
            return isEmpty();
        }

        private boolean closedState() {
            return isClosed();
        }
    }

    private File createLogFile() throws IOException {
        File file = File.createTempFile("jpom-tail-watcher-test", ".log");
        file.deleteOnExit();
        Files.write(file.toPath(), Arrays.asList("line-1", "line-2"), StandardCharsets.UTF_8);
        return file;
    }

    @Test
    public void testAddRemoveAndCloseIfEmpty() throws Exception {
        TestWatcher watcher = new TestWatcher(createLogFile());
        FakeSession first = new FakeSession("session-1");
        FakeSession second = new FakeSession("session-2");
        assertTrue(watcher.addSession(first));
        assertTrue(watcher.addSession(second));
        // 相同会话重复添加返回 false（调用方视为已监听），不重复计数
        assertFalse(watcher.addSession(first));
        assertEquals(2, watcher.sessionCount());

        assertTrue(watcher.removeSession(first));
        assertFalse(watcher.closedState());
        assertTrue(watcher.removeSession(second));
        assertTrue(watcher.empty());

        watcher.closeIfEmptyForTest();
        assertTrue(watcher.closedState());
        // 关闭后不再接受新会话，调用方应重新获取/创建 watcher
        assertFalse(watcher.addSession(new FakeSession("session-3")));
        // 重复 closeIfEmpty/close 必须幂等
        watcher.closeIfEmptyForTest();
        watcher.closeForTest();
    }

    @Test
    public void testSendFailureRemovesSession() throws Exception {
        TestWatcher watcher = new TestWatcher(createLogFile());
        FakeSession broken = new FakeSession("broken", true);
        boolean failed = false;
        try {
            watcher.addSession(broken);
        } catch (IOException expected) {
            failed = true;
        }
        assertTrue("发送失败应向上抛出，由调用方结束会话", failed);
        // 关键断言：发送失败时不能把会话留在集合里（否则会话与 watcher 一起泄漏）
        assertEquals(0, watcher.sessionCount());
        assertTrue(watcher.empty());
        watcher.closeForTest();
    }

    @Test
    public void testCloseClosesAllSessions() throws Exception {
        TestWatcher watcher = new TestWatcher(createLogFile());
        FakeSession first = new FakeSession("close-1");
        FakeSession second = new FakeSession("close-2");
        assertTrue(watcher.addSession(first));
        assertTrue(watcher.addSession(second));
        watcher.closeForTest();
        assertTrue(watcher.closedState());
        assertTrue(first.isClosed());
        assertTrue(second.isClosed());
        assertEquals(0, watcher.sessionCount());
    }

    /**
     * 多线程并发 add/remove/closeIfEmpty，同时日志文件持续写入触发 Tailer 发送，
     * 用于守住 ConcurrentModificationException、关闭竞态与重复关闭 Tailer 问题。
     */
    @Test
    public void testConcurrentAddRemoveWithTailer() throws Exception {
        File logFile = createLogFile();
        TestWatcher watcher = new TestWatcher(logFile);
        int threadCount = 6;
        int iterations = 120;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount + 1);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        // 持续写日志，驱动 Tailer 线程与 add/remove 并发
        futures.add(pool.submit(() -> {
            start.await();
            for (int i = 0; i < iterations; i++) {
                Files.write(logFile.toPath(), ("tail-" + i + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                    java.nio.file.StandardOpenOption.APPEND);
                Thread.sleep(2);
            }
            return null;
        }));
        for (int t = 0; t < threadCount; t++) {
            final int threadIndex = t;
            futures.add(pool.submit(() -> {
                start.await();
                for (int i = 0; i < iterations; i++) {
                    FakeSession session = new FakeSession("t" + threadIndex + "-" + i);
                    try {
                        watcher.addSession(session);
                    } catch (IOException ignored) {
                        // watcher 可能已被其他线程关闭，忽略
                    }
                    watcher.removeSession(session);
                }
                return null;
            }));
        }
        start.countDown();
        for (Future<?> future : futures) {
            future.get(60, TimeUnit.SECONDS);
        }
        pool.shutdownNow();
        // 收尾：关闭幂等，且不应残留会话
        watcher.closeIfEmptyForTest();
        watcher.closeForTest();
        assertEquals(0, watcher.sessionCount());
        assertTrue(watcher.closedState());
    }
}
