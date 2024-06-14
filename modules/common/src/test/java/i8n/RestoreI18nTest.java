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
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Lombok;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
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
        // 提取中文
        ExtractI18nTest.walkFile(rootFile, file1 -> {
            try {
                for (Pattern pattern : ExtractI18nTest.messageKeyPatterns) {
                    restoreChineseInFile(file1, pattern, zhProperties);
                }
            } catch (Exception e) {
                throw Lombok.sneakyThrow(e);
            }
        });
    }

    private void restoreChineseInFile(File file, Pattern pattern, Properties zhProperties) throws Exception {
        StringWriter writer = new StringWriter();
        boolean modified = false;
        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), charset)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (ExtractI18nTest.canIgnore(line)) {
                    writer.write(line);
                } else {
                    // 将 i18n key 替换为中文
                    StringBuffer modifiedLine = new StringBuffer();
                    Matcher matcher = pattern.matcher(line);
                    if (StrUtil.containsAny(line, ExtractI18nTest.JpomAnnotation)) {
                        if (matcher.find()) {
                            String key = matcher.group(1);
                            if (ExtractI18nTest.needIgnoreCase(key, line)) {
                                String chineseText = (String) zhProperties.get(key);
                                if (chineseText == null) {
                                    throw new IllegalArgumentException("找不到对应的中文:" + key);
                                }
                                // 完整替换
                                modifiedLine.append(StrUtil.replace(line, String.format("\"%s\"", key), String.format("\"%s\"", chineseText)));
                            } else {
                                modifiedLine.append(line);
                            }
                        } else {
                            modifiedLine.append(line);
                        }
                    } else {
                        while (matcher.find()) {
                            String key = matcher.group(1);
                            if (!ExtractI18nTest.needIgnoreCase(key, line)) {
                                continue;
                            }
                            String chineseText = (String) zhProperties.get(key);
                            if (chineseText == null) {
                                throw new IllegalArgumentException("找不到对应的中文:" + key);
                            }
                            // 正则关键词替换
                            matcher.appendReplacement(modifiedLine, String.format("\"%s\"", chineseText));
                        }
                        matcher.appendTail(modifiedLine);
                    }
                    String lineString = modifiedLine.toString();
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
}
