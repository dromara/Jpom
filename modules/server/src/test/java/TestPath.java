import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import org.junit.Test;
import org.springframework.util.AntPathMatcher;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author bwcx_jzy
 * @date 2019/8/24
 */
public class TestPath {

    public static void main(String[] args) {
        AntPathMatcher antPathMatcher = new AntPathMatcher();
        System.out.println(antPathMatcher.match("/s/**/sss.html", "//s/s/s/sss.html"));
    }

    @Test
    public void test() throws MalformedURLException {
        URL url = new URL("jar:file:/home/jpom/server/lib/server-2.4.8.jar!/BOOT-INF/classes!/");
        String file = url.getFile();
        String x = StrUtil.subBefore(file, "!", false);
        System.out.println(x);
        System.out.println(FileUtil.file(x));

        System.out.println(StrUtil.subBefore("sssddsf", "!", false));

    }
}
