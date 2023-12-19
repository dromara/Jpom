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
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

/**
 * @author bwcx_jzy
 * @since 2022/11/25
 */
@Slf4j
public class TestSynchronized {


    /**
     * 死锁测试
     */
    @Test
    void deadLockTests() throws InterruptedException {

        for (int i = 0; i < 100000; i++) {
            User u1 = new User(50);
            User u2 = new User(50);
            log.info("第 - {} - 次", i);
            Thread thread = new Thread(() -> u1.hit(u2), "A");
            Thread thread1 = new Thread(() -> u2.hit(u1), "B");
            thread1.start();
            thread.start();

            thread1.join();
            thread.join();
        }

    }


    private static int i = 0;

    static class User {
        public int num;
        private final Lock lock = new Lock(++i);

        public User(int num) {
            this.num = num;
        }

        public void sub(int step) {
            log.info("我是{} 我的锁是: {} ,我准备拿sub锁 {} ", Thread.currentThread().getName(), lock, lock);
            synchronized (lock) {
                log.info("我是{} 拿到的sub锁为: {}", Thread.currentThread().getName(), lock);
                this.num = this.num - step;
            }
            log.info("我是{} 我的锁是: {} 我释放了sub锁:{}", Thread.currentThread().getName(), lock, lock);
        }

        public void hit(User user) {
            log.info("我是{} 我的锁是: {},我准备拿hit锁 {} ", Thread.currentThread().getName(), lock, lock);
            synchronized (lock) {
                log.info("我是{} 拿到hit锁的锁为: {}", Thread.currentThread().getName(), lock);
                if (user != null) {
                    user.sub(10);
                }
            }
            log.info("我是{} 我的锁是: {},我释放了hit锁 {} ", Thread.currentThread().getName(), lock, lock);
        }

    }

    static class Lock {
        private final int i;

        Lock(int i) {
            this.i = i;
        }

        @Override
        public String toString() {
            return "LOCK-" + i;
        }
    }

}
