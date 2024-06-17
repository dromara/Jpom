/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package i8n;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.PageUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import i8n.api.BaiduBceRpcTexttransTest;
import i8n.api.VolcTranslateApiTest;
import lombok.Lombok;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * <h1>Jpom 后端 i18n 工具自动化流程</h1>
 * <p>
 * <ol>
 *     <li>提取代码中的所有关联的中文</li>
 *     <li>将提取到的中文使用千帆大模型（百度）进行语意化翻译</li>
 *     <li>将翻译结果进行拼接成 key = i18n.xxxx.md5('原始中文')[4+]</li>
 *     <li>将结果缓存至 words.json (如果缓存中存在不会进行重复翻译)</li>
 *     <li>----------------------------</li>
 *     <li>将 words.json 转换为 zh_CN.properties</li>
 *     <li>扫描代码中所有关联中文并根据 zh_CN.properties 进行替换</li>
 *     <li>对比扫描结果中使用到的 i18n key，删除 zh_CN.properties 、words.json 中未关联的 key</li>
 *     <li>对应扫描结果中的 i18n key 但是 zh_CN.properties 中不存在的 key，进行提示</li>
 *     <li>----------------------------</li>
 *     <li>将 zh_CN.properties 使用火山翻译（字节跳动）转换为 en_US.properties</li>
 *     <li>----------------------------</li>
 *     <li>使用 idea 优化 import 实现自动导包</li>
 * </ol>
 * <p>
 *   ======================================
 * <p>
 *  扫描代码中的中文关键词：\"[\u4e00-\u9fa5]+\"
 *
 * @author bwcx_jzy
 * @since 2024/6/11
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ExtractI18nTest {

    /**
     * 项目根路径
     */
    private File rootFile;

    private File zhPropertiesFile;
    private final Charset charset = CharsetUtil.CHARSET_UTF_8;
    /**
     * 匹配中文字符的正则表达式
     */
    private final Pattern[] chinesePatterns = new Pattern[]{
        // 中文开头
        Pattern.compile("\"[\\u4e00-\\u9fa5][\\u4e00-\\u9fa5\\w.,;:'!?()~，><#@$%{}【】、（）：\\[\\]+\" \\-。～！=/|]*\""),
        // 序号开头
        Pattern.compile("\"\\d+\\..*[\\u4e00-\\u9fa5][\\u4e00-\\u9fa5\\w.,;:'!?()~，><#@$%{}【】、（）：\\[\\]+\" \\-。]*\""),
        // 符合开头
        Pattern.compile("\"[,;:'!?()~，><#@$%{}【】、（）：\\[\\]+\" \\-。].*[\\u4e00-\\u9fa5][\\u4e00-\\u9fa5\\w.,;:'!?()~，><#@$%{}【】、（）：\\[\\]+\" \\-。]*\""),
        // 空格开头
        Pattern.compile("\"[\\s+][\\u4e00-\\u9fa5][\\u4e00-\\u9fa5\\w.,;:'!?()~，><#@$%{}【】、（）：\\[\\]+\" \\-。]*\""),
        Pattern.compile("\"[a-zA-Z.·\\d][\\u4e00-\\u9fa5]*[\\u4e00-\\u9fa5.,;:'!?()~，><#@$%{}【】、（）：\\[\\]+\" \\-。]*\""),
        Pattern.compile("\"[\\d.]\\s[\\u4e00-\\u9fa5]*[\\u4e00-\\u9fa5.,;:'!?()~，><#@$%{}【】、（）：\\[\\]+\" \\-。]*\""),
        Pattern.compile("\"[\\u4e00-\\u9fa5]+[a-zA-Z]\""),
        // 字母开头
        Pattern.compile("\"[a-zA-Z{} ].*[\\u4e00-\\u9fa5][\\u4e00-\\u9fa5\\w.,;:'!?()~，><#@$%{}【】、（）：\\[\\]+\" \\-。]*\""),
        Pattern.compile("\"[a-zA-Z{} ].*[\\d\\s].*[\\u4e00-\\u9fa5][\\u4e00-\\u9fa5\\w.,;:'!?()~，><#@$%{}【】、（）：\\[\\]+\" \\-。]*\""),
    };
    /**
     * 代码中关联（引用） key 的正则
     */
    public static final Pattern[] messageKeyPatterns = new Pattern[]{
        // 优先匹配（避免后续替换异常）
        Pattern.compile("TransportI18nMessageUtil\\.get\\(\"(.*?)\"\\)"),
        Pattern.compile("I18nMessageUtil\\.get\\(\"(.*?)\"\\)"),
        Pattern.compile("@ValidatorItem\\(.*?msg\\s*=\\s*\"([^\"]*)\".*?\\)"),
        Pattern.compile("nameKey\\s*=\\s*\"([^\"]*)\".*?\\)"),
    };

    public final static String[] JpomAnnotation = {
        "@ValidatorItem", "nameKey = \""
    };

    @Before
    public void before() throws Exception {
        File file = new File("");
        String rootPath = file.getAbsolutePath();
        rootFile = new File(rootPath).getParentFile();
        //
        zhPropertiesFile = FileUtil.file(rootFile, "common/src/main/resources/i18n/messages_zh_CN.properties");
    }

    /**
     * 提取代码中的中文并使用大模型进行语意化翻译 key
     */
    @Test
    public void extractJavaZh_First1() throws Exception {
        // 中文字符串
        Set<String> wordsSet = new LinkedHashSet<>();
        // 将已经存在的合并使用, 中文资源文件存储路径
        Properties zhProperties = new Properties();
        try (BufferedReader inputStream = FileUtil.getReader(zhPropertiesFile, charset)) {
            zhProperties.load(inputStream);
        }
        zhProperties.values().forEach(o -> wordsSet.add(o.toString()));
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
        // 语意化中文存储为 key（排序）
        Collection<String> wordsSetSort = CollUtil.sort(wordsSet, String::compareTo);
        //
        JSONObject cacheWords = this.loadCacheWords();

        int pageSize = 50;
        int total = CollUtil.size(wordsSet);
        int page = PageUtil.totalPage(total, pageSize);
        JSONObject allResult = new JSONObject();
        //
        for (int i = PageUtil.getFirstPageNo(); i <= page; i++) {
            int start = PageUtil.getStart(i, pageSize);
            int end = PageUtil.getEnd(i, pageSize);
            Collection<String> sub = CollUtil.sub(wordsSetSort, start, end);
            Collection<String> subAfter = filterExists(sub, cacheWords, allResult);
            if (CollUtil.isNotEmpty(subAfter)) {
                while (true) out:{
                    BaiduBceRpcTexttransTest bceRpcTexttrans = new BaiduBceRpcTexttransTest();
                    System.out.println("等待翻译：" + subAfter);
                    JSONObject jsonObject = bceRpcTexttrans.doTranslate(subAfter);
                    System.out.println("翻译结果：" + jsonObject);
                    // 转换为可用 key
                    for (Map.Entry<String, Object> entry : jsonObject.entrySet()) {
                        String key = entry.getKey();
                        String value = (String) entry.getValue();
                        String originalValue = findOriginal(subAfter, value);
                        if (originalValue == null) {
                            System.err.println("翻译后的中文和翻译前的中文不一致（需要重试）：" + value);
                            break out;
                        }
                        String buildKey = this.buildKey(key, originalValue, allResult);
                        allResult.put(buildKey, originalValue);
                    }
                    break;
                }
            }
            saveWords(allResult, true);
        }
    }

    /**
     * 根据提取出的中文生成 i18n 中文配置文件、并替换代码中的关键词
     * <p>
     * 1. 自动对比未关联
     * 2. 自动删除未使用
     *
     * @throws Exception 异常
     */
    @Test
    public void generateZhPropertiesAndReplace_Second2() throws Exception {
        JSONObject cacheWords = this.loadCacheWords();
        TreeMap<String, Object> sort = MapUtil.sort(cacheWords);
        Properties zhProperties = new Properties();
        zhProperties.putAll(sort);
        {
            Properties zhExitsProperties = new Properties();
            try (BufferedReader inputStream = FileUtil.getReader(zhPropertiesFile, charset)) {
                zhExitsProperties.load(inputStream);
            }
            if (!ObjectUtil.equals(zhExitsProperties, zhProperties)) {
                // 不一致才重新存储
                try (BufferedWriter writer = FileUtil.getWriter(zhPropertiesFile, charset, false)) {
                    zhProperties.store(writer, "i18n zh");
                }
            }
        }
        // 将配置按照中文转 map
        /*
          中文对应的 key map
          <p>
          key:中文
          value:随机key
         */
        Map<String, String> chineseMap = new HashMap<>();
        for (Map.Entry<Object, Object> entry : zhProperties.entrySet()) {
            chineseMap.put(StrUtil.toStringOrNull(entry.getValue()), StrUtil.toStringOrNull(entry.getKey()));
        }
        // 代码中已经使用到的 key
        Collection<Object> useKeys = new HashSet<>();
        // 替换中文
        walkFile(rootFile, file1 -> {
            try {
                for (Pattern chinesePattern : chinesePatterns) {
                    replaceQuotedChineseInFile(file1, chinesePattern, chineseMap, useKeys);
                }
            } catch (Exception e) {
                throw Lombok.sneakyThrow(e);
            }
        });
        //
        boolean isChange = false;
        for (Object keyObj : CollUtil.newArrayList(zhProperties.keySet())) {
            String key = StrUtil.toStringOrNull(keyObj);
            if (useKeys.contains(key)) {
                continue;
            }
            System.err.println("配置中存在未关联的key（将自动删除 zhProperties、words.json）:" + key);
            zhProperties.remove(key);
            cacheWords.remove(key);
            isChange = true;
        }
        if (isChange) {
            saveWords(cacheWords, false);
            try (BufferedWriter writer = FileUtil.getWriter(zhPropertiesFile, charset, false)) {
                zhProperties.store(writer, "i18n zh");
            }
        }
        List<Object> notUseKeys = useKeys.stream().filter(o -> !zhProperties.containsKey(o)).collect(Collectors.toList());
        Assert.assertTrue("存在未使用的 key:" + CollUtil.join(notUseKeys, StrUtil.COMMA), CollUtil.isEmpty(notUseKeys));
    }

    /**
     * <a href="https://www.volcengine.com/docs/4640/65067">https://www.volcengine.com/docs/4640/65067</a>
     *
     * @throws IOException io 异常
     */
    @Test
    public void syncZhToEnProperties_Third3() throws Exception {
        // 加载中文配置
        Properties zhProperties = new Properties();
        try (BufferedReader inputStream = FileUtil.getReader(zhPropertiesFile, charset)) {
            zhProperties.load(inputStream);
        }
        Map<String, String> map = new HashMap<>();
        map.put("en_US", "en");
        map.put("zh_HK", "zh-Hant-hk");
        map.put("zh_TW", "zh-Hant-tw");
        for (Map.Entry<String, String> entry : map.entrySet()) {

            this.syncZhToProperties(entry.getKey(), entry.getValue());
        }
    }

    private void syncZhToProperties(String language, String volcLang) throws Exception {
        // 加载中文配置
        Properties zhProperties = new Properties();
        try (BufferedReader inputStream = FileUtil.getReader(zhPropertiesFile, charset)) {
            zhProperties.load(inputStream);
        }

        // 加载配置
        File enPropertiesFile = FileUtil.file(rootFile, "common/src/main/resources/i18n/messages_" + language + ".properties");
        Properties enEexitsProperties = new Properties();
        try (BufferedReader inputStream = FileUtil.getReader(enPropertiesFile, charset)) {
            enEexitsProperties.load(inputStream);
        }
        // 翻译成目标语言
        VolcTranslateApiTest translateApi = new VolcTranslateApiTest();
        Set<Object> keySets = zhProperties.keySet();
        // 接口限制、不能超过 16
        int pageSize = 16;
        int total = CollUtil.size(keySets);
        int page = PageUtil.totalPage(total, pageSize);
        //
        Properties enProperties = new Properties();
        for (int i = PageUtil.getFirstPageNo(); i <= page; i++) {
            int start = PageUtil.getStart(i, pageSize);
            int end = PageUtil.getEnd(i, pageSize);

            List<Object> keys = CollUtil.sub(keySets, start, end);
            List<String> values = new ArrayList<>();
            List<String> useKeys = new ArrayList<>();
            for (Object object : keys) {
                String key = (String) object;
                String value1 = (String) zhProperties.get(key);
                String existsValue = (String) enEexitsProperties.get(key);
                if (existsValue != null) {
                    // 已经存在
                    enProperties.put(key, existsValue);
                    continue;
                }
                values.add(value1);
                useKeys.add(key);
            }
            if (CollUtil.isNotEmpty(values)) {
                JSONArray translateText = translateApi.translate("zh", volcLang, values);
                System.out.println(values);
                System.out.println(translateText);
                System.out.println("=================");
                for (int i1 = 0; i1 < values.size(); i1++) {
                    enProperties.put(useKeys.get(i1), translateText.getJSONObject(i1).getString("Translation"));
                }
            }
        }
        if (!ObjectUtil.equals(enEexitsProperties, enProperties)) {
            try (BufferedWriter writer = FileUtil.getWriter(enPropertiesFile, charset, false)) {
                enProperties.store(writer, "i18n " + language);
            }
        }
    }

    /**
     * 过滤已经存在的语意 key 的中文
     *
     * @param wordsSet   中文数组
     * @param cacheWords 已经存在的语意 key
     * @param allResult  新的结果
     * @return 过滤后的中文
     */
    private Collection<String> filterExists(Collection<String> wordsSet, JSONObject cacheWords, JSONObject allResult) {
        return wordsSet.stream()
            .filter(s -> {
                String existKey = checkExists(cacheWords, s);
                if (existKey != null) {
                    //System.out.println("key 已经存在：" + existKey);
                    allResult.put(existKey, s);
                    return false;
                }
                return true;
            })
            .collect(Collectors.toList());
    }

    /**
     * 判断对应的中文是否存在语意 key
     *
     * @param jsonObject 已经存在的语意 key
     * @param value      中文
     * @return 语意 key
     */
    private String checkExists(JSONObject jsonObject, String value) {
        for (Map.Entry<String, Object> entry : jsonObject.entrySet()) {
            String entryValue = (String) entry.getValue();
            if (StrUtil.equals(entryValue, value)) {
                // 值相同
                String key = entry.getKey();
                List<String> split = StrUtil.split(key, StrUtil.DOT);
                String last = CollUtil.getLast(split);
                String md5 = SecureUtil.md5(value);
                if (StrUtil.startWith(md5, last)) {
                    // 可以的后缀也等于值的 md5 前缀
                    return key;
                }
                System.err.println("翻译前后的值相等但是 md5 不一致：" + md5 + "," + last);
            }
        }
        return null;
    }

    /**
     * 增量更新缓存的 words
     *
     * @param jsonObject 需要更新的信息
     * @param append     是否增量更新
     */
    private void saveWords(JSONObject jsonObject, boolean append) {
        File wordsFile = FileUtil.file(rootFile, "common/src/main/resources/i18n/words.json");
        if (append) {
            JSONObject cacheWords = this.loadCacheWords();
            JSONObject updateAfter = cacheWords.clone();
            updateAfter.putAll(jsonObject);
            if (!ObjectUtil.equals(MapUtil.sort(updateAfter), MapUtil.sort(cacheWords))) {
                // 变动才保存
                // 根据 key 排序
                TreeMap<String, Object> sort = MapUtil.sort(updateAfter);
                FileUtil.writeString(JSONArray.toJSONString(sort, JSONWriter.Feature.PrettyFormat), wordsFile, charset);
            }
        } else {
            TreeMap<String, Object> sort = MapUtil.sort(jsonObject);
            FileUtil.writeString(JSONArray.toJSONString(sort, JSONWriter.Feature.PrettyFormat), wordsFile, charset);
        }
    }

    private JSONObject loadCacheWords() {
        File wordsFile = FileUtil.file(rootFile, "common/src/main/resources/i18n/words.json");
        if (wordsFile.exists()) {
            return JSONObject.parseObject(FileUtil.readUtf8String(wordsFile));
        }
        return new JSONObject();
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

    /**
     * 找到原始的中文字符串（大模型处理后面前后空格可能不存在）
     *
     * @param list  list（翻译前）
     * @param value value（翻译后）
     * @return 原始的中文字符串 ，null 不存在
     */
    private String findOriginal(Collection<String> list, String value) {
        for (String s : list) {
            if (StrUtil.equals(s, value) || StrUtil.equals(StrUtil.trim(s), value)) {
                // 需要返回原始值，不能返回翻译后的值
                return s;
            }
        }
        return null;
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
            if (StrUtil.containsAny(path, "/test/", "\\test\\")) {
                return;
            }
            if (StrUtil.equals("java", FileUtil.extName(file1))) {
                consumer.accept(file1);
            }
        });
    }


    /**
     * 替换代码中的中文为方法调用
     *
     * @param file    java 文件
     * @param pattern 当前匹配的正则
     * @throws IOException io 异常
     */
    private void replaceQuotedChineseInFile(File file, Pattern pattern, Map<String, String> chineseMap, Collection<Object> useKeys) throws Exception {
        //String subPath = FileUtil.subPath(rootFile.getAbsolutePath(), file);
        // 先存储于临时文件
        boolean modified = false;
        StringWriter writer = new StringWriter();
        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), charset)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (canIgnore(line)) {
                    writer.write(line);
                } else {
                    // 匹配已经使用到的 key
                    for (Pattern messageKeyPattern : messageKeyPatterns) {
                        Matcher matcher = messageKeyPattern.matcher(line);
                        while (matcher.find()) {
                            String key = matcher.group(1);
                            if (!needIgnoreCase(key, line)) {
                                continue;
                            }
                            useKeys.add(key);
                        }
                    }
                    // 替换为 i18n key 或者方法
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
                                matcher.appendReplacement(modifiedLine, String.format("TransportI18nMessageUtil.get(\"%s\")", key));
                            } else {
                                matcher.appendReplacement(modifiedLine, String.format("I18nMessageUtil.get(\"%s\")", key));
                            }
                        }
                        useKeys.add(key);
                    }
                    matcher.appendTail(modifiedLine);
                    String lineString = modifiedLine.toString();
                    if (canIgnore(lineString)) {
                        throw new IllegalStateException("替换后成为忽略行：" + line + " \n" + lineString);
                    }
                    writer.write(lineString);
                    if (!modified) {
                        modified = !StrUtil.equals(line, lineString);
                    }
                }
                writer.write(FileUtil.getLineSeparator());
            }
        }
        if (modified) {
            // 移动到原路径
            FileUtil.writeString(writer.toString(), file, charset);
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
     * @return 是否需要忽略 (不是 中文 true)
     */
    public static boolean needIgnoreCase(String text, String line) {
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
    public static boolean canIgnore(String line) throws ClassNotFoundException {
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
