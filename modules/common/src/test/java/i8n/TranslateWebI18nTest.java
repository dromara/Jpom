package i8n;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.PageUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import i8n.api.VolcTranslateApiTest;
import org.junit.Test;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author bwcx_jzy
 * @since 2024/6/17
 */
public class TranslateWebI18nTest {

    Map<String, Object> waitMap = new HashMap<>();

    // 接口限制、不能超过 16
    int pageSize = 16;


    @Test
    public void test() throws Exception {
        File file = new File("");
        String rootPath = file.getAbsolutePath();
        File rootFile = new File(rootPath).getParentFile().getParentFile();

        JSONObject zhCn = this.loadJson(rootFile, "zh_cn");
        JSONObject enUs = this.loadJson(rootFile, "en_us");


        JSONObject jsonObject1 = new JSONObject();
        this.generateWaitMap(zhCn, "", enUs, jsonObject1);
        //
        this.doTranslate(jsonObject1, "en");
        System.out.println(jsonObject1);
    }

    private void doTranslate(JSONObject result, String toLanguage) throws Exception {
        Set<Map.Entry<String, Object>> entries = waitMap.entrySet();
        VolcTranslateApiTest translateApi = new VolcTranslateApiTest();
        {
            int total = CollUtil.size(entries);
            int page = PageUtil.totalPage(total, pageSize);


            for (int i = PageUtil.getFirstPageNo(); i <= page; i++) {
                int start = PageUtil.getStart(i, pageSize);
                int end = PageUtil.getEnd(i, pageSize);

                List<Map.Entry<String, Object>> values2 = CollUtil.sub(entries, start, end);
                List<String> collected = values2.stream().map(entry -> (String) entry.getValue()).collect(Collectors.toList());
                if (CollUtil.isEmpty(collected)) {
                    continue;
                }
                JSONArray translateText = translateApi.translate("zh", toLanguage, collected);
                System.out.println(collected);
                System.out.println(translateText);
                System.out.println("=================");
                for (int i1 = 0; i1 < collected.size(); i1++) {
                    String key = values2.get(i1).getKey();
                    System.out.println(key);
                    BeanUtil.setProperty(result, key, translateText.getJSONObject(i1).getString("Translation"));
                    //enProperties.put(useKeys.get(i1), translateText.getJSONObject(i1).getString("Translation"));
                    System.out.println(result);
                }
            }
        }
    }

    private JSONObject loadJson(File rootFile, String tag) {
        File file1 = FileUtil.file(rootFile, "web-vue/src/i18n/locales/" + tag + ".json");
        if (!file1.exists()) {
            return new JSONObject();
        }
        return JSONObject.parseObject(FileUtil.readUtf8String(file1));
    }

    private void generateWaitMap(JSONObject jsonObject, String rootPath, JSONObject result, JSONObject cache) throws Exception {
        for (Map.Entry<String, Object> entry : jsonObject.entrySet()) {
            String key = entry.getKey();
            String keyPath = rootPath.isEmpty() ? key : rootPath + "." + key;
            Object value = entry.getValue();
            if (value instanceof JSONObject) {
                generateWaitMap((JSONObject) value, keyPath, result, cache);
            } else if (value instanceof String) {
                doGenerateWaitJson(jsonObject, rootPath, result, cache);
            } else {
                System.err.println("不支持的数据格式：" + keyPath);
            }
        }
    }

    private void doGenerateWaitJson(JSONObject jsonObject, String rootPath, JSONObject result, JSONObject cache) throws Exception {
        Set<String> keySet = jsonObject.keySet();
        Collection<Object> values = jsonObject.values();
        {
            int total = CollUtil.size(keySet);
            int page = PageUtil.totalPage(total, pageSize);
            for (int i = PageUtil.getFirstPageNo(); i <= page; i++) {
                int start = PageUtil.getStart(i, pageSize);
                int end = PageUtil.getEnd(i, pageSize);

                List<Object> values2 = CollUtil.sub(values, start, end);
                List<String> keySet2 = CollUtil.sub(keySet, start, end);

                for (int i1 = 0; i1 < keySet2.size(); i1++) {
                    String key = keySet2.get(i1);
                    String keyPath = rootPath + "['" + key + "']";
                    Object propertyVal = BeanUtil.getProperty(cache, keyPath);
                    if (propertyVal != null) {
                        // 已经存在
                        BeanUtil.setProperty(result, keyPath, propertyVal);
                        continue;
                    }
                    waitMap.put(keyPath, values2.get(i1));
                }
            }
        }

    }

    @Test
    public void testJson() {
        JSONObject jsonObject = new JSONObject();
        BeanUtil.setProperty(jsonObject, "test.a", "123");
        System.out.println(jsonObject);
    }
}
