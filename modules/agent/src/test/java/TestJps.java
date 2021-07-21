import cn.hutool.core.text.StrSplitter;
import cn.hutool.core.util.StrUtil;
import io.jpom.util.CommandUtil;
import org.junit.Test;

import java.util.List;

/**
 * @author bwcx_jzy
 * @date 2019/9/8
 */
public class TestJps {
    @Test
    public void test() {
        String execSystemCommand = CommandUtil.execSystemCommand("jps -v");
        List<String> list = StrSplitter.splitTrim(execSystemCommand, StrUtil.LF, true);
        for (String item : list) {
            System.out.println("******************************");
            System.out.println(item);
        }
    }
}
