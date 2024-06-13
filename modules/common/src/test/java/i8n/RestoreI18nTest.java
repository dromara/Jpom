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

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Lombok;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 还原 i18n
 *
 * @author bwcx_jzy
 * @since 2024/6/13
 */
public class RestoreI18nTest {
    private final Charset charset = CharsetUtil.CHARSET_UTF_8;
    private File rootFile;
    private File zhPropertiesFile;

    @Before
    public void before() throws Exception {
        File file = new File("");
        String rootPath = file.getAbsolutePath();
        rootFile = new File(rootPath).getParentFile();
        //
        zhPropertiesFile = FileUtil.file(rootFile, "common/src/main/resources/i18n/messages_zh_CN.properties");
    }

    @Test
    public void test() throws IOException {
        Properties zhProperties = new Properties();
        try (BufferedReader inputStream = FileUtil.getReader(zhPropertiesFile, charset)) {
            zhProperties.load(inputStream);
        }
        // 临时文件
        File tempDir = FileUtil.file(rootFile, "i18n-temp");
        // 提取中文
        ExtractI18nTest.walkFile(rootFile, file1 -> {
            try {
                for (Tuple tuple : ExtractI18nTest.messageKeyPatterns) {
                    restoreChineseInFile(file1, tempDir, tuple, zhProperties);
                }
            } catch (Exception e) {
                throw Lombok.sneakyThrow(e);
            }
        });
    }

    private void restoreChineseInFile(File file, File tempDir, Tuple tuple, Properties zhProperties) throws Exception {
        String subPath = FileUtil.subPath(rootFile.getAbsolutePath(), file);
        // 先存储于临时文件
        File tempFile = FileUtil.file(tempDir, subPath);
        FileUtil.mkParentDirs(tempFile);
        boolean modified = false;
        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8);
             BufferedWriter writer = Files.newBufferedWriter(tempFile.toPath())) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (ExtractI18nTest.canIgnore(line)) {
                    writer.write(line);
                } else {
//                    // 匹配已经使用到的 key
//                    for (Pattern messageKeyPattern : ExtractI18nTest.messageKeyPatterns) {
//                        Matcher matcher = messageKeyPattern.matcher(line);
//                        while (matcher.find()) {
//                            String key = matcher.group(1);
//                            if (!ExtractI18nTest.needIgnoreCase(key, line)) {
//                                continue;
//                            }
//                            useKeys.add(key);
//                        }
//                    }
                    // 替换为 i18n key 或者方法

                    Pattern pattern = tuple.get(0);
                    boolean full = tuple.get(1);
                    Matcher matcher = pattern.matcher(line);
                    StringBuffer modifiedLine = new StringBuffer();
                    if (full) {
                        while (matcher.find()) {
                            String key = matcher.group(1);
                            if (!ExtractI18nTest.needIgnoreCase(key, line)) {
                                continue;
                            }
                            String chineseText = (String) zhProperties.get(key);
                            if (chineseText == null) {
                                throw new IllegalArgumentException("找不到对应的中文:" + key);
                            }
                            //System.out.println("需要单独处理的：" + line);
                            // 完整替换
                            matcher.appendReplacement(modifiedLine, String.format("\"%s\"", chineseText));
                        }
                        matcher.appendTail(modifiedLine);
                    } else {
                        if (matcher.find()) {
                            String key = matcher.group(1);
                            if (ExtractI18nTest.needIgnoreCase(key, line)) {
                                String chineseText = (String) zhProperties.get(key);
                                if (chineseText == null) {
                                    throw new IllegalArgumentException("找不到对应的中文:" + key);
                                }
                                modifiedLine.append(StrUtil.replace(line, String.format("\"%s\"", key), String.format("\"%s\"", chineseText)));
                            } else {
                                modifiedLine.append(line);
                            }
                        } else {
                            modifiedLine.append(line);
                        }
                    }
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
}
