package i8n.web;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.PageUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
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


        Map<String, String> map = new HashMap<>();
        map.put("en_us", "en");
        map.put("zh_hk", "zh-Hant-hk");
        map.put("zh_tw", "zh-Hant-tw");
        for (Map.Entry<String, String> entry : map.entrySet()) {

            this.syncZhToJson(rootFile, zhCn, entry.getKey(), entry.getValue());
        }
    }

    private void syncZhToJson(File rootFile, JSONObject zhCn, String tag, String toLanguage) throws Exception {
        waitMap.clear();
        JSONObject cache = this.loadJson(rootFile, tag);
        //
        JSONObject jsonObject1 = new JSONObject();
        this.generateWaitMap(zhCn, "", jsonObject1, cache);
        //
        this.doTranslate(jsonObject1, toLanguage);
        jsonObject1 = this.sort(jsonObject1);
        File file1 = FileUtil.file(rootFile, "web-vue/src/i18n/locales/" + tag + ".json");
        FileUtil.writeString(JSONArray.toJSONString(jsonObject1, JSONWriter.Feature.PrettyFormat), file1, CharsetUtil.CHARSET_UTF_8);
    }

    private JSONObject sort(JSONObject jsonObject) {
        Set<Map.Entry<String, Object>> entries = jsonObject.entrySet();
        for (Map.Entry<String, Object> entry : entries) {
            Object value = entry.getValue();
            if (value instanceof String) {
                //
            } else if (value instanceof JSONObject) {
                value = this.sort((JSONObject) value);
                entry.setValue(value);
            }
        }

        return new JSONObject(MapUtil.sort(jsonObject));
    }

    private void doTranslate(JSONObject result, String toLanguage) throws Exception {
        Set<Map.Entry<String, Object>> entries = waitMap.entrySet();
        VolcTranslateApiTest translateApi = new VolcTranslateApiTest();
        int total = CollUtil.size(entries);
        int page = PageUtil.totalPage(total, pageSize);
        for (int i = PageUtil.getFirstPageNo(); i <= page; i++) {
            int start = PageUtil.getStart(i, pageSize);
            int end = PageUtil.getEnd(i, pageSize);

            List<Map.Entry<String, Object>> values2 = CollUtil.sub(entries, start, end);
            if (CollUtil.isEmpty(values2)) {
                continue;
            }
            List<String> collected = values2.stream().map(entry -> (String) entry.getValue()).collect(Collectors.toList());

            JSONArray translateText = translateApi.translate("zh", toLanguage, collected);
            System.out.println(collected);
            System.out.println(translateText);
            System.out.println("=================");
            for (int i1 = 0; i1 < collected.size(); i1++) {
                String keyPath = values2.get(i1).getKey();
                String translation = translateText.getJSONObject(i1).getString("Translation");
                this.setData(result, keyPath, translation);
            }
        }
    }

    private void setData(JSONObject result, String keyPath, Object data) {
        List<String> split = StrUtil.splitTrim(keyPath, ".");
        JSONObject groupValue = result;
        for (int i = 0; i < split.size() - 1; i++) {
            String key = split.get(i);
            JSONObject groupValue2 = groupValue.getJSONObject(key);
            if (groupValue2 == null) {
                groupValue2 = new JSONObject();
                groupValue.put(key, groupValue2);
            }
            groupValue = groupValue2;
        }
        groupValue.put(CollUtil.getLast(split), data);
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

        int total = CollUtil.size(keySet);
        int page = PageUtil.totalPage(total, pageSize);
        for (int i = PageUtil.getFirstPageNo(); i <= page; i++) {
            int start = PageUtil.getStart(i, pageSize);
            int end = PageUtil.getEnd(i, pageSize);

            List<Object> values2 = CollUtil.sub(values, start, end);
            List<String> keySet2 = CollUtil.sub(keySet, start, end);

            for (int i1 = 0; i1 < keySet2.size(); i1++) {
                String key = keySet2.get(i1);
                String keyPath = rootPath + "." + key;
                Object propertyVal = BeanUtil.getProperty(cache, keyPath);
                if (propertyVal instanceof String) {
                    // 已经存在
                    setData(result, keyPath, propertyVal);
                    continue;
                } else {
                    //System.err.println("数据类型不正确：" + keyPath + "  " + propertyVal);
                }
                Object value = values2.get(i1);
                if ((value instanceof String)) {
                    waitMap.put(keyPath, value);
                } else if (value instanceof JSONObject) {
                    generateWaitMap((JSONObject) value, rootPath, result, cache);
                } else {
                    throw new Exception("不支持的数据格式：" + keyPath + "  " + value);
                }
            }
        }
    }
}
