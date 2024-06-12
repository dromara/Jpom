package i8n;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Lombok;
import lombok.SneakyThrows;
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

/**
 * @author bwcx_jzy
 * @since 2024/6/11
 */
public class ExtractTest {

    private Collection<String> wordsSet = new LinkedHashSet<>();
    private final Map<String, String> chineseMap = new HashMap<>();
    private final Collection<Object> useKeys = new HashSet<>();
    private File rootFile;


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

    private void generateKey(File file) throws IOException {
        //   /Users/user/IdeaProjects/Jpom/jpom-parent/modules/.DS_Store
        File zhPropertiesFile = FileUtil.file(file, "common/src/main/resources/i18n/messages_zh_CN.properties");
        Properties zhProperties = new Properties();
        Charset charset = CharsetUtil.CHARSET_UTF_8;
        try (BufferedReader inputStream = FileUtil.getReader(zhPropertiesFile, charset)) {
            zhProperties.load(inputStream);
        }
        Collection<Object> oldKeys = zhProperties.keySet();
        Collection<Object> linkUsed = new LinkedHashSet<>();
        wordsSet = CollUtil.sort(wordsSet, String::compareTo);
        wordsSet.forEach(s -> {
            String key = null;
            for (Map.Entry<Object, Object> entry : zhProperties.entrySet()) {
                if (StrUtil.equals(StrUtil.toStringOrNull(entry.getValue()), s)) {
                    key = (String) entry.getKey();
                    break;
                }
            }
            if (key == null) {
                do {
                    key = StrUtil.format("key.{}", RandomUtil.randomStringUpper(4));
                } while (zhProperties.containsKey(key));
                System.out.println("生成新的 key:" + key);
                zhProperties.put(key, s);
            }
            linkUsed.add(key);
        });
        // 删除不存在的
        int beforeSize = oldKeys.size();
        oldKeys.removeIf(next -> {
            //
            return !linkUsed.contains(next) && !useKeys.contains(next);
        });
        int afterSize = oldKeys.size();
        if (beforeSize != afterSize) {
            System.out.println(beforeSize + "  " + afterSize);
        }

        for (Object useKey : useKeys) {
            if (zhProperties.containsKey(useKey)) {
                continue;
            }
            System.out.println("存在未关联的key:" + useKey);
        }

        try (BufferedWriter writer = FileUtil.getWriter(zhPropertiesFile, charset, false)) {
            zhProperties.store(writer, "i18n zh");
        }
        System.out.println(zhProperties.size());

        for (Map.Entry<Object, Object> entry : zhProperties.entrySet()) {
            chineseMap.put(StrUtil.toStringOrNull(entry.getValue()), StrUtil.toStringOrNull(entry.getKey()));
        }
    }

    @Test
    @SneakyThrows
    public void extract() {
        File file = new File("");
        String rootPath = file.getAbsolutePath();
        rootFile = file = new File(rootPath).getParentFile();
        // 删除临时文件
        FileUtil.del(FileUtil.file(rootFile, "i18n-temp"));
        // 提取中文
        walkFile(file, file1 -> {
            try {
                for (Pattern chinesePattern : chinesePatterns) {
                    verifyDuplicates(file1, chinesePattern);
                    extractFile(file1, chinesePattern);
                }
            } catch (IOException e) {
                throw Lombok.sneakyThrow(e);
            }
        });
        // 生成 key
        generateKey(file);
        // 替换中文
//        walkFile(file, file1 -> {
//            try {
//                for (Pattern chinesePattern : chinesePatterns) {
//                    replaceQuotedChineseInFile(file1, chinesePattern);
//                }
//            } catch (IOException e) {
//                throw Lombok.sneakyThrow(e);
//            }
//        });
    }

    // 匹配中文字符的正则表达式
    Pattern[] chinesePatterns = new Pattern[]{
        Pattern.compile("\"[\\u4e00-\\u9fa5][\\u4e00-\\u9fa5\\w.,;:'!?()~，><#@$%{}【】、（）：\\[\\]+\" \\-]*\""),
        Pattern.compile("\" [\\u4e00-\\u9fa5][\\u4e00-\\u9fa5\\w.,;:'!?()~，><#@$%{}【】、（）\\[\\]+\" \\-]*\""),
        Pattern.compile("\"[a-zA-Z][\\w\\u4e00-\\u9fa5]*[\\u4e00-\\u9fa5]\""),
        Pattern.compile("\"[\\u4e00-\\u9fa5]+[a-zA-Z]\""),
    };
    Pattern[] messageKeyPatterns = new Pattern[]{
        Pattern.compile("MessageUtil\\.get\\(\"(.*?)\"\\)"),
        Pattern.compile("TransportMessageUtil\\.get\\(\"(.*?)\"\\)"),
    };


    private void replaceQuotedChineseInFile(File file, Pattern pattern) throws IOException {
        String subPath = FileUtil.subPath(rootFile.getAbsolutePath(), file);
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
                        String unWrap = StrUtil.unWrap(chineseText, '\"');
                        String key = chineseMap.get(unWrap);
                        if (key == null) {
                            throw new IllegalArgumentException("找不到 key:" + unWrap);
                        }
                        if (StrUtil.contains(line, "@ValidatorItem(")) {
                            System.out.println("需要单独处理的：" + line);
                            matcher.appendReplacement(modifiedLine, String.format("\"%s\"", key));
                        } else {
                            String path = FileUtil.getAbsolutePath(file);
                            if (StrUtil.containsAny(path, "/agent-transport-common/", "\\agent-transport-common\\")) {
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
            FileUtil.move(tempFile, file, true);
        } else {
            FileUtil.del(tempFile);
        }
    }

    private void verifyDuplicates(File file, Pattern pattern) throws IOException {
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
                    int count = StrUtil.count(chineseText, '\"');
                    if (count > 2) {
                        System.err.println(line);
                        throw new IllegalArgumentException("重复的 key:" + chineseText);
                    }
                    find = true;
                }

                if (find && StrUtil.contains(line, "@ValidatorItem(")) {
                    System.out.println("需要单独处理的：" + line);
                }
            }
        }
    }

    private void extractFile(File file, Pattern pattern) throws IOException {
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
                        wordsSet.add(StrUtil.unWrap(chineseText, '\"'));
                        System.out.println("匹配到的内容：" + chineseText + "  -> " + line.trim());
                    }
                }
                boolean found = false;
                for (Pattern messageKeyPattern : messageKeyPatterns) {
                    Matcher matcher = messageKeyPattern.matcher(line);
                    while (matcher.find()) {
                        useKeys.add(matcher.group(1));
                        found = true;
                    }
                }
                if (found) {
                    continue;
                }
            }
        }
    }

    private boolean canIgnore(String line) {
        String trimLin = line.trim();
        if (StrUtil.startWithAny(trimLin, "@ValidatorItem")) {
            return false;
        }
        if (StrUtil.startWithAny(trimLin, "log.", "@", "*", "//", "public static final")) {

            return true;
        }
        if (StrUtil.endWithAny(trimLin, "),")) {
            // 枚举
//            System.out.println(trimLin);
            return true;
        }
        return false;
    }
}
