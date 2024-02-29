/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package cn;

import cn.hutool.core.util.IdUtil;
import cn.hutool.http.HttpUtil;
import org.databene.contiperf.PerfTest;
import org.databene.contiperf.junit.ContiPerfRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.UUID;

/**
 * @author bwcx_jzy
 * @since 2019/9/3
 */
public class ContiPerTest {
    @Rule
    public ContiPerfRule i = new ContiPerfRule();

//    @Test
    @Test
    @PerfTest(invocations = 200000000, threads = 16)
    public void test1() throws Exception {
        IdUtil.fastSimpleUUID();
    }


//    @Test
    @PerfTest(invocations = 200000000, threads = 16)
    public void test2() throws Exception {
        UUID.randomUUID().toString();
    }

//    @Test
    @PerfTest(invocations = 20000, threads = 16)
    public void testHttp() {
        HttpUtil.createGet("https://baidu.com")
                .timeout(2000)
                .execute();
    }
}
