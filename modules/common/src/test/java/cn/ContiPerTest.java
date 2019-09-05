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
 * @date 2019/9/3
 */
public class ContiPerTest {
    @Rule
    public ContiPerfRule i = new ContiPerfRule();

//    @Test
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
