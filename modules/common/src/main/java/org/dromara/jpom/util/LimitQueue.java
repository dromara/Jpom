/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.util;

import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 定长队列
 *
 * @author bwcx_jzy
 * @since 2019/3/16
 */
public class LimitQueue<E> extends ConcurrentLinkedDeque<E> {
    private final int limit;

    public LimitQueue(int limit) {
        this.limit = Math.max(limit, 0);
    }

    @Override
    public boolean offerFirst(E s) {
        if (full()) {
            // 删除最后一个
            pollLast();
        }
        return super.offerFirst(s);
    }

    @Override
    public boolean offerLast(E s) {
        if (full()) {
            // 删除第一个
            pollFirst();
        }
        return super.offerLast(s);
    }

    public boolean full() {
        return size() > limit;
    }
}
