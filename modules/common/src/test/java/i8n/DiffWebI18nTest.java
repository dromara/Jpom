package i8n;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.alibaba.fastjson2.JSONObject;
import lombok.Lombok;
import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

/**
 * @author bwcx_jzy
 * @since 2024/6/18
 */
public class DiffWebI18nTest {

    Pattern pattern = Pattern.compile("t\\('(.*?)'\\)");

    @Test
    public void test() throws Exception {
        File file = new File("");
        String rootPath = file.getAbsolutePath();
        File rootFile = new File(rootPath).getParentFile().getParentFile();
        rootFile = FileUtil.file(rootFile, "web-vue");
        JSONObject zhCn = this.loadJson(rootFile, "zh_cn");
        Map<String, String> cacheKey = new HashMap<>();
        this.generateWaitMap(zhCn, "", cacheKey);
        Collection<String> values = cacheKey.values();
        HashSet<String> set = new HashSet<>(values);
        Map<String, String> chinese = new HashMap<>();
        for (String s : set) {
            int frequency = 1;
            while (true) {
                CRC32 crc32 = new CRC32();
                crc32.update((s + frequency).getBytes());
                String crc32Value = Long.toHexString(crc32.getValue()).toUpperCase();
                String newKey = StrUtil.format("i18n.{}.{}", crc32Value, frequency);
                String existsKey = chinese.get(s);
                if (existsKey == null) {
                    chinese.put(s, newKey);
                    break;
                }
                if (StrUtil.equals(newKey, existsKey)) {
                    break;
                }
                frequency++;
            }
        }
        //
        Map<String, String> newKeyMap = new HashMap<>(chinese.size());
        cacheKey.forEach((key, value) -> {
            String newKey = chinese.get(value);
            newKeyMap.put(key, newKey);
        });
        walkFile(rootFile, file1 -> {
            try {
                matchFile(file1, pattern, newKeyMap);
            } catch (Exception e) {
                throw Lombok.sneakyThrow(e);
            }
        });
    }

    /**
     * 生成 key
     *
     * @param key        翻译后的key
     * @param value      原始中文
     * @param jsonObject 所以的key
     * @return i18n.{}.{}
     */
    private String buildKey(String key, String value, JSONObject jsonObject) {
        int md5IdLen = 4;
        while (true) {
            String md5 = SecureUtil.md5(value);
            Assert.assertTrue("截取中文 md5 key 超范围：" + value, md5.length() >= md5IdLen);
            md5 = md5.substring(0, md5IdLen);
            String newKey = StrUtil.format("i18n.{}.{}", StrUtil.toUnderlineCase(key), md5);
            if (jsonObject.containsKey(newKey)) {
                md5IdLen += 2;
                continue;
            }
            return newKey;
        }
    }

    private void generateWaitMap(JSONObject jsonObject, String rootPath, Map<String, String> cache) throws Exception {
        for (Map.Entry<String, Object> entry : jsonObject.entrySet()) {
            String key = entry.getKey();
            String keyPath = rootPath.isEmpty() ? key : rootPath + "." + key;
            Object value = entry.getValue();
            if (value instanceof JSONObject) {
                generateWaitMap((JSONObject) value, keyPath, cache);
            } else if (value instanceof String) {
                cache.put(keyPath, (String) value);
            } else {
                System.err.println("不支持的数据格式：" + keyPath);
            }
        }
    }

    private JSONObject loadJson(File rootFile, String tag) {
        File file1 = FileUtil.file(rootFile, "/src/i18n/locales/" + tag + ".json");
        if (!file1.exists()) {
            return new JSONObject();
        }
        return JSONObject.parseObject(FileUtil.readUtf8String(file1));
    }


    /**
     * 扫描指定目录下所有 java 文件（忽略 test、i18n-temp 目录）
     *
     * @param file     目录
     * @param consumer java 文件
     */
    public static void walkFile(File file, Consumer<File> consumer) {
        FileUtil.walkFiles(file, file1 -> {
            if (FileUtil.isDirectory(file1)) {
                return;
            }
            String path = FileUtil.getAbsolutePath(file1);
            String normalize = FileUtil.normalize(path);
            if (StrUtil.containsAny(normalize, "/node_modules/")) {
                return;
            }
            if (StrUtil.equalsAny(FileUtil.extName(file1), "ts", "js", "vue")) {
                consumer.accept(file1);
            }
        });
    }

    private void matchFile(File file, Pattern pattern, Map<String, String> newKeyMap) throws Exception {
        StringWriter writer = new StringWriter();
        boolean modified = false;
        Charset charset = CharsetUtil.CHARSET_UTF_8;
        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), charset)) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);
                StringBuffer modifiedLine = new StringBuffer();
                while (matcher.find()) {
                    String key = matcher.group(1);
                    int start = matcher.start(1);
                    String subPre = StrUtil.subPre(line, start);
                    if (StrUtil.endWithAny(subPre, "split('", "import('", "$emit('", "onSubmit('")) {
                        // 特殊方法
                        continue;
                    }
                    String newKey = newKeyMap.get(key);
                    // 正则关键词替换
                    matcher.appendReplacement(modifiedLine, newKey);
                    System.out.println(key + "  " + newKey);
                }
                matcher.appendTail(modifiedLine);
                //
                String lineString = modifiedLine.toString();
                writer.write(lineString);
                if (!modified) {
                    modified = !StrUtil.equals(line, lineString);
                }
                writer.write(FileUtil.getLineSeparator());
            }
        }
        if (modified) {
            // 移动到原路径
            FileUtil.writeString(writer.toString(), file, charset);
        }
    }
}
