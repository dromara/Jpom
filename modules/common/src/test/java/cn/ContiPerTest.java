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
