/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.transport.tunnel.ws;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 由隧道数据帧驱动的输入流（用于 {@code download} 原语的流式响应）。
 * <p>
 * 帧到达即入队、消费方按需读取，不会把整个响应体缓存在内存里。
 *
 * @author jpom
 * @since 2026/11/18
 */
public class TunnelInputStream extends InputStream {

    private static final byte[] EOF = new byte[0];

    private final LinkedBlockingQueue<byte[]> queue = new LinkedBlockingQueue<>();

    private volatile Throwable error;
    private byte[] current;
    private int position;

    /**
     * 收到一段数据
     *
     * @param data 数据
     */
    void offer(byte[] data) {
        if (data != null && data.length > 0) {
            queue.offer(data);
        }
    }

    /**
     * 对端发送完毕
     */
    void finish() {
        queue.offer(EOF);
    }

    /**
     * 对端异常/中止
     *
     * @param throwable 异常
     */
    void fail(Throwable throwable) {
        this.error = throwable;
        queue.offer(EOF);
    }

    @Override
    public int read() throws IOException {
        if (!ensure()) {
            return -1;
        }
        int value = current[position++] & 0xFF;
        if (position >= current.length) {
            current = null;
            position = 0;
        }
        return value;
    }

    @Override
    public int read(byte[] bytes, int off, int len) throws IOException {
        if (len == 0) {
            return 0;
        }
        if (!ensure()) {
            return -1;
        }
        int count = Math.min(len, current.length - position);
        System.arraycopy(current, position, bytes, off, count);
        position += count;
        if (position >= current.length) {
            current = null;
            position = 0;
        }
        return count;
    }

    @Override
    public int available() {
        byte[] data = current;
        return data == null ? 0 : data.length - position;
    }

    @Override
    public void close() {
        queue.clear();
        queue.offer(EOF);
    }

    private boolean ensure() throws IOException {
        while (true) {
            Throwable throwable = error;
            if (throwable != null && (current == null || position >= current.length)) {
                if (throwable instanceof IOException) {
                    throw (IOException) throwable;
                }
                throw new IOException(throwable.getMessage(), throwable);
            }
            if (current != null && position < current.length) {
                return true;
            }
            current = null;
            position = 0;
            byte[] data;
            try {
                data = queue.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("读取隧道响应被中断", e);
            }
            if (data == EOF || data.length == 0) {
                return false;
            }
            current = data;
        }
    }
}
