package i8n;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.PageUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import com.alibaba.fastjson2.JSONArray;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * <a href="https://www.volcengine.com/docs/4640/65067">https://www.volcengine.com/docs/4640/65067</a>
 *
 * @author bwcx_jzy
 * @since 2024/6/11
 */
public class TranslateEnTest {


    String SecretAccessKey;
    String AccessKeyID;

    @Before
    public void before() {
        String volcSk = SystemUtil.get("JPOM_TRANSLATE_VOLC_SK", StrUtil.EMPTY);
        String volcAk = SystemUtil.get("JPOM_TRANSLATE_VOLC_AK", StrUtil.EMPTY);
        Assert.assertNotEquals("请配置火山翻译 SecretAccessKey[JPOM_TRANSLATE_VOLC_SK]", volcSk, StrUtil.EMPTY);
        Assert.assertNotEquals("请配置火山翻译 AccessKeyID[JPOM_TRANSLATE_VOLC_AK]", volcAk, StrUtil.EMPTY);
        SecretAccessKey = volcSk;
        AccessKeyID = volcAk;
    }

    @Test
    public void test() throws Exception {
        File file = new File("");
        String rootPath = file.getAbsolutePath();
        File rootFile = new File(rootPath).getParentFile();

        VolcTranslateApi translateApi = new VolcTranslateApi(AccessKeyID, SecretAccessKey);


        File zhPropertiesFile = FileUtil.file(rootFile, "common/src/main/resources/i18n/messages_zh_CN.properties");
        Properties zhProperties = new Properties();
        Charset charset = CharsetUtil.CHARSET_UTF_8;
        try (BufferedReader inputStream = FileUtil.getReader(zhPropertiesFile, charset)) {
            zhProperties.load(inputStream);
        }

        Set<Object> objects = zhProperties.keySet();
        int size = CollUtil.size(objects);
        int page = PageUtil.totalPage(size, 16);
        //
        Properties enProperties = new Properties();
        for (int i = 0; i < page; i++) {
            int start = PageUtil.getStart(i, 16);
            int end = PageUtil.getEnd(i, 16);

            List<Object> sub = CollUtil.sub(objects, start, end);
            List<String> values = new ArrayList<>();
            for (Object object : sub) {
                String key = (String) object;
                String value1 = (String) zhProperties.get(key);
                values.add(value1);
            }


            JSONArray translateText = translateApi.translate("zh", "en", values);

            System.out.println(values);
            System.out.println(translateText);
            System.out.println("=================");
            for (int i1 = 0; i1 < sub.size(); i1++) {
                enProperties.put(sub.get(i1), translateText.getJSONObject(i1).getString("Translation"));
            }
        }
        File enPropertiesFile = FileUtil.file(rootFile, "common/src/main/resources/i18n/messages_en_US.properties");
        try (BufferedWriter writer = FileUtil.getWriter(enPropertiesFile, charset, false)) {
            enProperties.store(writer, "i18n en");
        }
    }

    @Test
    public void test2() throws Exception {
        VolcTranslateApi translateApi = new VolcTranslateApi(AccessKeyID, SecretAccessKey);
        JSONArray translateText = translateApi.translate("zh", "en", CollUtil.newArrayList("你好", "世界"));
        System.out.println(translateText);
    }
}
