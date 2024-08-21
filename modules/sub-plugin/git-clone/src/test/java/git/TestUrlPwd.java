package git;

import cn.hutool.core.util.URLUtil;
import org.junit.Test;

/**
 * @author bwcx_jzy
 * @since 2024/8/14
 */
public class TestUrlPwd {

    @Test
    public void test() {
        System.out.println(URLUtil.encodeAll("sdfsdf@aa;:"));
    }
}
