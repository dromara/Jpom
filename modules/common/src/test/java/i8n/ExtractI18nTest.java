package i8n;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.PageUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.Lombok;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 提交代码中的中文并生成随机 key 转存到 properties
 * <p>
 * \"[\u4e00-\u9fa5]+\"
 *
 * @author bwcx_jzy
 * @since 2024/6/11
 */
public class ExtractI18nTest {
    /**
     * 中文对应的 key map
     * <p>
     * key:中文
     * value:随机key
     */
    private final Map<String, String> chineseMap = new HashMap<>();
    /**
     * 代码中已经使用到的 key
     */
    private final Collection<Object> useKeys = new HashSet<>();
    /**
     * 项目根路径
     */
    private File rootFile;

    private final Properties zhProperties = new Properties();
    /**
     * 匹配中文字符的正则表达式
     */
    private final Pattern[] chinesePatterns = new Pattern[]{
        // 中文开头
        Pattern.compile("\"[\\u4e00-\\u9fa5][\\u4e00-\\u9fa5\\w.,;:'!?()~，><#@$%{}【】、（）：\\[\\]+\" \\-。]*\""),
        // 序号开头
        Pattern.compile("\"\\d+\\..*[\\u4e00-\\u9fa5][\\u4e00-\\u9fa5\\w.,;:'!?()~，><#@$%{}【】、（）：\\[\\]+\" \\-。]*\""),
        // 符合开头
        Pattern.compile("\"[,;:'!?()~，><#@$%{}【】、（）：\\[\\]+\" \\-。][\\u4e00-\\u9fa5][\\u4e00-\\u9fa5\\w.,;:'!?()~，><#@$%{}【】、（）：\\[\\]+\" \\-。]*\""),
        // 空格开头
        Pattern.compile("\"[\\s+][\\u4e00-\\u9fa5][\\u4e00-\\u9fa5\\w.,;:'!?()~，><#@$%{}【】、（）：\\[\\]+\" \\-。]*\""),
        Pattern.compile("\"[a-zA-Z.·\\d][\\u4e00-\\u9fa5]*[\\u4e00-\\u9fa5.,;:'!?()~，><#@$%{}【】、（）：\\[\\]+\" \\-。]*\""),
        Pattern.compile("\"[\\d.]\\s[\\u4e00-\\u9fa5]*[\\u4e00-\\u9fa5.,;:'!?()~，><#@$%{}【】、（）：\\[\\]+\" \\-。]*\""),
        Pattern.compile("\"[\\u4e00-\\u9fa5]+[a-zA-Z]\""),
        // 字母开头
        Pattern.compile("\"[a-zA-Z{} ].*[\\u4e00-\\u9fa5]\""),
    };
    /**
     * 代码中关联（引用） key 的正则
     */
    private final Pattern[] messageKeyPatterns = new Pattern[]{
        Pattern.compile("MessageUtil\\.get\\(\"(.*?)\"\\)"),
        Pattern.compile("TransportMessageUtil\\.get\\(\"(.*?)\"\\)"),
        Pattern.compile("@ValidatorItem\\(.*?msg\\s*=\\s*\"([^\"]*)\".*?\\)"),
        Pattern.compile("nameKey\\s*=\\s*\"([^\"]*)\".*?\\)"),
    };

    private final String[] JpomAnnotation = {
        "@ValidatorItem", "nameKey = \""
    };

    @Before
    public void before() throws Exception {
        File file = new File("");
        String rootPath = file.getAbsolutePath();
        rootFile = new File(rootPath).getParentFile();
        //
        // 中文资源文件存储路径
        File zhPropertiesFile = FileUtil.file(rootFile, "common/src/main/resources/i18n/messages_zh_CN.properties");
        Charset charset = CharsetUtil.CHARSET_UTF_8;
        try (BufferedReader inputStream = FileUtil.getReader(zhPropertiesFile, charset)) {
            zhProperties.load(inputStream);
        }
    }

    @Test
    public void extract() {
        // 删除临时文件
        FileUtil.del(FileUtil.file(rootFile, "i18n-temp"));
        // 中文字符串
        Set<String> wordsSet = new LinkedHashSet<>();
        // 提取中文
        walkFile(rootFile, file1 -> {
            try {
                for (Pattern chinesePattern : chinesePatterns) {
                    verifyDuplicates(file1, chinesePattern);
                    extractFile(file1, chinesePattern, wordsSet);
                }
            } catch (Exception e) {
                throw Lombok.sneakyThrow(e);
            }
        });
        // 检查去除前后空格后是否重复
        Map<String, Long> collect = wordsSet.stream()
            .map(StrUtil::trim)
            .collect(Collectors.groupingBy(e -> e, Collectors.counting()));
        for (Map.Entry<String, Long> entry : collect.entrySet()) {
            long value = entry.getValue();
            Assert.assertEquals("[" + entry.getKey() + "]出现去重空格后重复", 1L, value);
        }
        // 语意化中文存储为 key
        Collection<String> wordsSetSort = CollUtil.sort(wordsSet, String::compareTo);
        int pageSize = 50;
        int total = CollUtil.size(wordsSet);
        int page = PageUtil.totalPage(total, pageSize);
        JSONObject allResult = new JSONObject();
        //
        for (int i = PageUtil.getFirstPageNo(); i <= page; i++) {
            int start = PageUtil.getStart(i, pageSize);
            int end = PageUtil.getEnd(i, pageSize);

            List<String> sub = CollUtil.sub(wordsSetSort, start, end);
            while (true) out:{
                BaiduBceRpcTexttransTest bceRpcTexttrans = new BaiduBceRpcTexttransTest();
                JSONObject jsonObject = bceRpcTexttrans.doTranslate(sub);
                System.out.println("翻译结果：" + jsonObject);
                // 转换为可用 key
                for (Map.Entry<String, Object> entry : jsonObject.entrySet()) {
                    String key = entry.getKey();
                    String value = (String) entry.getValue();
                    String originalValue = findOriginal(sub, value);
                    if (originalValue == null) {
                        System.err.println("翻译后的中文和翻译前的中文不一致（需要重试）：" + value);
                        break out;
                    }
                    String buildKey = this.buildKey(key, originalValue, allResult);
                    allResult.put(buildKey, originalValue);

                }
                // 提前保存
                File wordsFile = FileUtil.file(rootFile, "common/src/main/resources/i18n/words.json");
                FileUtil.writeString(JSONArray.toJSONString(allResult), wordsFile, StandardCharsets.UTF_8);
                break;
            }
        }
    }

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

    /**
     * 找到原始的中文字符串（大模型处理后面前后空格可能不存在）
     *
     * @param list  list
     * @param value value
     * @return 原始的中文字符串 ，null 不存在
     */
    private String findOriginal(List<String> list, String value) {
        for (String s : list) {
            if (StrUtil.equals(s, value) || StrUtil.equals(StrUtil.trim(s), value)) {
                return value;
            }
        }
        return null;
    }

    @Test
    public void replace() {
        // 替换中文
        walkFile(rootFile, file1 -> {
            try {
                for (Pattern chinesePattern : chinesePatterns) {
                    replaceQuotedChineseInFile(file1, chinesePattern);
                }
            } catch (Exception e) {
                throw Lombok.sneakyThrow(e);
            }
        });
    }

    /**
     * 扫描指定目录下所有 java 文件（忽略 test、i18n-temp 目录）
     *
     * @param file     目录
     * @param consumer java 文件
     */
    private void walkFile(File file, Consumer<File> consumer) {
        FileUtil.walkFiles(file, file1 -> {
            if (FileUtil.isDirectory(file1)) {
                return;
            }
            String path = FileUtil.getAbsolutePath(file1);
            if (StrUtil.containsAny(path, "/test/", "/i18n-temp/", "\\test\\", "\\i18n-temp\\")) {
                return;
            }
            if (StrUtil.equals("java", FileUtil.extName(file1))) {
                consumer.accept(file1);
            }
        });
    }

    /**
     * 生成中文对应的 key
     *
     * @param file 项目根路径
     * @throws IOException io 异常
     */
    private void generateKey(File file) throws IOException {
        //   /Users/user/IdeaProjects/Jpom/jpom-parent/modules/.DS_Store
//        Collection<Object> oldKeys = zhProperties.keySet();
//        Collection<Object> linkUsed = new LinkedHashSet<>();
//        wordsSet = CollUtil.sort(wordsSet, String::compareTo);
//        wordsSet.forEach(s -> {
//            // 根据中文反查 key
//            String key = null;
//            for (Map.Entry<Object, Object> entry : zhProperties.entrySet()) {
//                if (StrUtil.equals(StrUtil.toStringOrNull(entry.getValue()), s)) {
//                    key = (String) entry.getKey();
//                    break;
//                }
//            }
//            if (key == null) {
//                do {
//                    key = StrUtil.format("i18n.{}", RandomUtil.randomStringUpper(6));
//                } while (zhProperties.containsKey(key));
//                System.out.println("生成新的 key:" + key);
//                zhProperties.put(key, s);
//            }
//            linkUsed.add(key);
//        });
//        // 删除不存在的
//        int beforeSize = oldKeys.size();
//        oldKeys.removeIf(next -> {
//            //
//            boolean b = !linkUsed.contains(next) && !useKeys.contains(next);
//            if (b) {
//                System.out.println("删除 key：" + next);
//            }
//            return b;
//        });
//        int afterSize = oldKeys.size();
//        if (beforeSize != afterSize) {
//            System.out.println(beforeSize + "  " + afterSize);
//        }
//
//        for (Object useKey : useKeys) {
//            if (zhProperties.containsKey(useKey)) {
//                continue;
//            }
//            System.out.println("存在未关联的key:" + useKey);
//        }
//
//        try (BufferedWriter writer = FileUtil.getWriter(zhPropertiesFile, charset, false)) {
//            zhProperties.store(writer, "i18n zh");
//        }
//        System.out.println(zhProperties.size());
//
//        for (Map.Entry<Object, Object> entry : zhProperties.entrySet()) {
//            chineseMap.put(StrUtil.toStringOrNull(entry.getValue()), StrUtil.toStringOrNull(entry.getKey()));
//        }
    }

    /**
     * 替换代码中的中文为方法调用
     *
     * @param file    java 文件
     * @param pattern 当前匹配的正则
     * @throws IOException io 异常
     */
    private void replaceQuotedChineseInFile(File file, Pattern pattern) throws Exception {
        String subPath = FileUtil.subPath(rootFile.getAbsolutePath(), file);
        // 先存储于临时文件
        File tempFile = FileUtil.file(rootFile, "i18n-temp", subPath);
        FileUtil.mkParentDirs(tempFile);
        boolean modified = false;
        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8);
             BufferedWriter writer = Files.newBufferedWriter(tempFile.toPath())) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (canIgnore(line)) {
                    writer.write(line);
                } else {
                    StringBuffer modifiedLine = new StringBuffer();
                    Matcher matcher = pattern.matcher(line);
                    while (matcher.find()) {
                        String chineseText = matcher.group();
                        if (needIgnoreCase(chineseText, line)) {
                            continue;
                        }
                        String unWrap = StrUtil.unWrap(chineseText, '\"');
                        String key = chineseMap.get(unWrap);
                        if (key == null) {
                            throw new IllegalArgumentException("找不到 key:" + unWrap);
                        }
                        if (StrUtil.containsAny(line, JpomAnnotation)) {
                            //System.out.println("需要单独处理的：" + line);
                            matcher.appendReplacement(modifiedLine, String.format("\"%s\"", key));
                        } else {
                            String path = FileUtil.getAbsolutePath(file);
                            if (StrUtil.containsAny(path, "/agent-transport/", "\\agent-transport\\")) {
                                matcher.appendReplacement(modifiedLine, String.format("TransportMessageUtil.get(\"%s\")", key));
                            } else {
                                matcher.appendReplacement(modifiedLine, String.format("MessageUtil.get(\"%s\")", key));
                            }
                        }
                    }
                    matcher.appendTail(modifiedLine);

                    writer.write(modifiedLine.toString());
                    modified = true;
                }
                writer.newLine();
            }
        }
        if (modified) {
            // 移动到原路径
            FileUtil.move(tempFile, file, true);
        } else {
            FileUtil.del(tempFile);
        }
    }

    /**
     * 验证拼接字符串
     * <p>
     * "aa"+abc+"xxxx"
     *
     * @param file    java 文件
     * @param pattern 匹配的正则
     * @throws IOException io 异常
     */
    private void verifyDuplicates(File file, Pattern pattern) throws Exception {
        try (BufferedReader reader = Files.newBufferedReader(file.toPath())) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (canIgnore(line)) {
                    continue;
                }
                //
                boolean find = false;

                Matcher matcher = pattern.matcher(line);
                while (matcher.find()) {
                    String chineseText = matcher.group();
                    if (needIgnoreCase(chineseText, line)) {
                        continue;
                    }
                    int count = StrUtil.count(chineseText, '\"');
                    if (count > 2) {
                        System.err.println(line);
                        throw new IllegalArgumentException("重复的 key:" + chineseText);
                    }
                    find = true;
                }

                if (find && StrUtil.containsAny(line, JpomAnnotation)) {
                    //System.out.println("需要单独处理的：" + line);
                }
            }
        }
    }

    /**
     * 提取文件中的中文
     *
     * @param file    java 文件
     * @param pattern 匹配的正则
     * @throws IOException io 异常
     */
    private void extractFile(File file, Pattern pattern, Collection<String> wordsSet) throws Exception {
        try (BufferedReader reader = Files.newBufferedReader(file.toPath())) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (canIgnore(line)) {
                    continue;
                }
                //
                {
                    Matcher matcher = pattern.matcher(line);
                    while (matcher.find()) {
                        String chineseText = matcher.group();
                        if (needIgnoreCase(chineseText, line)) {
                            continue;
                        }
                        wordsSet.add(StrUtil.unWrap(chineseText, '\"'));
                        System.out.println("匹配到的内容：" + chineseText + "  -> " + line.trim());
                    }
                }
                boolean found = false;
                for (Pattern messageKeyPattern : messageKeyPatterns) {
                    Matcher matcher = messageKeyPattern.matcher(line);
                    while (matcher.find()) {
                        String key = matcher.group(1);
                        if (!needIgnoreCase(key, line)) {
                            continue;
                        }
                        useKeys.add(key);
                        found = true;
                    }
                }
                if (found) {
                    continue;
                }
            }
        }
    }

    /**
     * 匹配到的结果是否需要忽略
     * <p>
     * 可能匹配到单字母（没有任何中文）
     *
     * @param text 匹配到的结果
     * @param line 整行
     * @return 是否需要忽略
     */
    private boolean needIgnoreCase(String text, String line) {
        Pattern pattern = Pattern.compile("[\\u4e00-\\u9fa5]");
        Matcher matcher = pattern.matcher(text);
        boolean b = matcher.find();
        if (!b) {
            //System.out.println("不包含汉字需要忽略：" + text + "    ======" + line);
            return true;
        }
        return false;
    }

    /**
     * 是否需要忽略
     *
     * @param line 代码行
     * @return 是否需要忽略
     */
    private boolean canIgnore(String line) throws ClassNotFoundException {
        String trimLin = line.trim();
        if (StrUtil.startWithAny(trimLin, JpomAnnotation)) {
            // jpom 特有注解
            return false;
        }
        if (StrUtil.startWithAny(trimLin, "@", "*", "//", "public static final String")) {
            // 注解、注释、枚举、产量
            return true;
        }
        if (StrUtil.endWithAny(trimLin, "),")) {
            // 枚举通用代码格式
            if (StrUtil.containsAny(trimLin, "() -> ")) {
                // 枚举实现了 Supplier
                return false;
            }
            // 假定枚举通用代码格式
            //  System.out.println(trimLin);
            return true;
        }
        return false;
    }
}
