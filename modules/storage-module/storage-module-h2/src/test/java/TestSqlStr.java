import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import org.junit.Test;

import java.io.InputStream;

/**
 * @author bwcx_jzy
 * @since 2023/1/6
 */
public class TestSqlStr {

    @Test
    public void test() {
        InputStream stream = ResourceUtil.getStream("sql/h2-db-v1.0.sql");
        String sql = IoUtil.readUtf8(stream);
        System.out.println(sql);
    }
}
