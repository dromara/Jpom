import cn.hutool.core.io.FileUtil;
import org.junit.Test;

/**
 * @author bwcx_jzy
 * @since 2023/3/14
 */
public class TestDeltete {

    @Test
    public void test() {
        boolean del = FileUtil.del("C:\\Users\\bwcx_\\jpom\\agent\\data\\project_file_backup");
        System.out.println(del);
    }

    @Test
    public void test2() {
        boolean del = FileUtil.del("D:\\data\\jpom\\a");
        System.out.println(del);
    }
}
