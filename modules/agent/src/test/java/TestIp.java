import cn.hutool.core.net.NetUtil;
import org.junit.Test;

import java.util.LinkedHashSet;

/**
 * @author bwcx_jzy
 * @date 2019/12/27
 */
public class TestIp {

    @Test
    public void test() {
        String ipByHost = NetUtil.getIpByHost("jpom.io");
        System.out.println(ipByHost);
    }

    @Test
    public void testAll() {
        LinkedHashSet<String> strings = NetUtil.localIpv4s();
        System.out.println(strings);
    }
}
