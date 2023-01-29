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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 根据执行KEY 多线程锁
 *
 * @author jiangzeyin
 * @since 2018/8/24.
 */
public class KeyLock<K> {
    /**
     * 保存所有锁定的KEY及其信号量
     */
    private final ConcurrentMap<K, LockInfo> map = new ConcurrentHashMap<>();

    /**
     * 获取锁的数量
     *
     * @return key总数
     */
    public int getLockKeyCount() {
        return map.size();
    }

    /**
     * 根据key 获取等待的线程数
     *
     * @param k k
     * @return 总数
     */
    public int getLockCount(K k) {
        LockInfo lockInfo = map.get(k);
        return lockInfo == null ? 0 : lockInfo.getLockCount();
    }


    /**
     * 释放key，唤醒其他等待此key的线程
     *
     * @param key key
     */
    public void unlock(K key) {
        if (key == null) {
            return;
        }
        LockInfo lockInfo = map.get(key);
        if (lockInfo == null) {
            return;
        }
        //  释放许可
        lockInfo.release();
        if (lockInfo.getLockCount() <= 0) {
            // 清除锁
            map.remove(key);
        }
    }

    /**
     * 锁定key，其他等待此key的线程将进入等待，直到调用{@link KeyLock#unlock}
     * 使用hashcode和equals来判断key是否相同，因此key必须实现{@link #hashCode()}和
     * {@link #equals(Object)}方法
     *
     * @param key key
     */
    public void lock(K key) {
        if (key == null) {
            return;
        }
        LockInfo lockInfo = map.computeIfAbsent(key, k -> new LockInfo());
        lockInfo.lock();
    }

    /**
     * 锁定多个key
     * 建议在调用此方法前先对keys进行排序，使用相同的锁定顺序，防止死锁发生
     *
     * @param keys keys
     */
    public void lock(K[] keys) {
        if (keys == null) {
            return;
        }
        for (K key : keys) {
            lock(key);
        }
    }

    /**
     * 释放多个key
     *
     * @param keys 多个keys
     */
    public void unlock(K[] keys) {
        if (keys == null) {
            return;
        }
        for (K key : keys) {
            unlock(key);
        }
    }

    /**
     * 锁的信息
     */
    private static class LockInfo {
        private final Semaphore semaphore;
        private final AtomicInteger lockCount = new AtomicInteger(0);

        private LockInfo() {
            this.semaphore = new Semaphore(1);
        }

        private void lock() {
            lockCount.getAndIncrement();
            semaphore.acquireUninterruptibly();
        }

        private void release() {
            semaphore.release();
            lockCount.getAndDecrement();
        }

        private int getLockCount() {
            return lockCount.get();
        }
    }
}
