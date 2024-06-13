package i8n;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.nio.charset.Charset;
import java.util.Properties;

/**
 *
 *
 * @author bwcx_jzy
 * @since 2024/6/11
 */
public class TranslateEnTest {


    @Test
    public void test() throws Exception {
        File file = new File("");
        String rootPath = file.getAbsolutePath();
        File rootFile = new File(rootPath).getParentFile();




        File zhPropertiesFile = FileUtil.file(rootFile, "common/src/main/resources/i18n/messages_zh_CN.properties");
        Properties zhProperties = new Properties();
        Charset charset = CharsetUtil.CHARSET_UTF_8;
        try (BufferedReader inputStream = FileUtil.getReader(zhPropertiesFile, charset)) {
            zhProperties.load(inputStream);
        }


    }
}
