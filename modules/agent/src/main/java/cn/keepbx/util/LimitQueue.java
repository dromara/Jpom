package cn.keepbx.util;

import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 定长队列
 *
 * @author jiangzeyin
 * @date 2019/3/16
 */
public class LimitQueue extends ConcurrentLinkedDeque<String> {
    private final int limit;

    public LimitQueue(int limit) {
        this.limit = limit;
    }

    @Override
    public boolean offerFirst(String s) {
        pollOver();
        return super.offerFirst(s);
    }

    @Override
    public boolean offerLast(String s) {
        pollOver();
        return super.offerLast(s);
    }

    private void pollOver() {
        if (full()) {
            poll();
        }
    }

    public boolean full() {
        return size() > limit;
    }
}
