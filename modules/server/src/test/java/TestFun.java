/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
import cn.hutool.core.util.StrUtil;
import org.junit.Test;
import org.springframework.util.Assert;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author bwcx_jzy
 * @since 2022/6/15
 */
public class TestFun {

    @Test
    public void tset() {
        // Assert.state(!StrUtil.equals("1", "1"), "not equal...");
        Assert.state(StrUtil.equals("1", "1"), "not equal...");
        List<Supplier<Boolean>> AFTER_CALLBACK = new LinkedList<>();
        AFTER_CALLBACK.add(() -> {
            System.out.println("1");
            return false;
        });
        AFTER_CALLBACK.add(() -> {
            System.out.println("2");
            return true;
        });
        AFTER_CALLBACK.add(() -> {
            System.out.println("3");
            return false;
        });
        long count = AFTER_CALLBACK.stream().mapToInt(value -> value.get() ? 1 : 0).count();
        System.out.println("123+" + count);
    }
}
