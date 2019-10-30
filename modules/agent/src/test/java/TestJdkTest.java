import cn.hutool.core.util.StrUtil;
import io.jpom.util.CommandUtil;
import io.jpom.util.FileUtils;
import org.junit.Test;

/**
 * @author bwcx_jzy
 * @date 2019/10/30
 */
public class TestJdkTest {

    @Test
    public void t() {
        String path = "C:\\Program Files\\Java\\jdk1.8.0_211";
        System.out.println(FileUtils.isJdkPath(path));
        //


        String version = FileUtils.getJdkVersion(path);
        System.out.println(version);

    }
}
