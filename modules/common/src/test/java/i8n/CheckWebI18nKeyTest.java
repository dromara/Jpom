package i8n;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import org.junit.Test;

import java.io.File;
import java.util.Map;

/**
 * @author bwcx_jzy
 * @since 2024/6/18
 */
public class CheckWebI18nKeyTest {

    @Test
    public void test() {
        File file = new File("");
        String rootPath = file.getAbsolutePath();
        File rootFile = new File(rootPath).getParentFile().getParentFile();
        rootFile = FileUtil.file(rootFile, "web-vue");
        JSONObject zhCn = DiffWebI18nTest.loadJson(rootFile, "zh_cn");
        for (Map.Entry<String, Object> entry : zhCn.entrySet()) {

            Object value = entry.getValue();
            if (value.toString().length() == 1) {
                System.out.println(StrUtil.format("错误的值：{}={}", entry.getKey(), value));
            }
        }
    }
}
