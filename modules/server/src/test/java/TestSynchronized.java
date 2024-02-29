/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
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
