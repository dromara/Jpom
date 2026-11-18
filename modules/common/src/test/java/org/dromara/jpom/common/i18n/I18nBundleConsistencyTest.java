/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.common.i18n;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * i18n 语言包完整性守卫。
 * <p>
 * 背景：2026-11-18 的 i18n 代码解析生成器（{@code i8n.ExtractI18nTest} 的 prune 步骤）在扫描不全时
 * 误删了约 1000 条在用条目，导致大量接口返回裸 key。生成器已弃用，语言包改为手工维护，
 * 本测试作为防线：代码里引用的 key 必须在四个语言包中齐全，四个语言包 key 集合必须一致。
 *
 * @author jpom
 * @since 2026/11/18
 */
public class I18nBundleConsistencyTest {

    private static final List<String> LOCALES = Arrays.asList("zh_CN", "en_US", "zh_HK", "zh_TW");

    /**
     * 代码中的 i18n key 字面量（只匹配 {@code "i18n.xxx"} 形式的常量，动态拼接的 key 不在此列）
     */
    private static final Pattern KEY_LITERAL = Pattern.compile("\"(i18n\\.[A-Za-z0-9_.]+)\"");

    private static final List<String> SKIP_DIRECTORIES = Arrays.asList("target", ".git", "node_modules", "web-vue",
        "go-agent", ".tmp", "dist", "backup");

    /**
     * 代码引用的 key 必须在四个语言包中都存在
     */
    @Test
    public void allReferencedKeysExistInEveryBundle() throws IOException {
        Path root = locateRepositoryRoot();
        Assume.assumeTrue("未找到完整源码树（仅在仓库内校验），跳过", root != null);
        Map<String, Set<String>> bundleKeys = new LinkedHashMap<>();
        for (String locale : LOCALES) {
            bundleKeys.put(locale, loadBundle(root, locale).keySet());
        }
        Set<String> referenced = scanReferencedKeys(root);
        Assert.assertFalse("未扫描到任何 i18n key 字面量，请检查扫描路径", referenced.isEmpty());
        List<String> problems = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : bundleKeys.entrySet()) {
            Set<String> missing = new TreeSet<>(referenced);
            missing.removeAll(entry.getValue());
            if (!missing.isEmpty()) {
                problems.add(entry.getKey() + " 缺少 " + missing.size() + " 个 key：" + preview(missing));
            }
        }
        Assert.assertTrue("代码引用的 key 未在语言包补齐：" + String.join("", problems), problems.isEmpty());
    }

    /**
     * 四个语言包 key 集合必须一致、没有重复 key、没有空值
     */
    @Test
    public void bundlesStayInSyncWithoutDuplicates() throws IOException {
        Path root = locateRepositoryRoot();
        Assume.assumeTrue("未找到完整源码树（仅在仓库内校验），跳过", root != null);
        Map<String, Map<String, String>> bundles = new LinkedHashMap<>();
        for (String locale : LOCALES) {
            bundles.put(locale, loadBundle(root, locale));
        }
        Map<String, String> zhCn = bundles.get("zh_CN");
        Assert.assertFalse("语言包为空：" + zhCn, zhCn.isEmpty());
        for (Map.Entry<String, Map<String, String>> entry : bundles.entrySet()) {
            Set<String> diff = new TreeSet<>(zhCn.keySet());
            diff.removeAll(entry.getValue().keySet());
            Assert.assertTrue(entry.getKey() + " 与 zh_CN 的 key 集合不一致，缺少：" + preview(diff), diff.isEmpty());
            Set<String> emptyValues = entry.getValue().entrySet().stream()
                .filter(item -> item.getValue() == null || item.getValue().trim().isEmpty())
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(TreeSet::new));
            Assert.assertTrue(entry.getKey() + " 存在空值：" + preview(emptyValues), emptyValues.isEmpty());
            long declared = declaredEntryCount(bundleFile(root, entry.getKey()));
            Assert.assertEquals(entry.getKey() + " 存在重复 key", entry.getValue().size(), declared);
        }
    }

    private static Map<String, String> loadBundle(Path root, String locale) throws IOException {
        Path file = bundleFile(root, locale);
        Assert.assertTrue("语言包不存在：" + file, Files.exists(file));
        Properties properties = new Properties();
        try (Reader reader = new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        Map<String, String> sorted = new LinkedHashMap<>();
        properties.stringPropertyNames().stream().sorted(Comparator.naturalOrder())
            .forEach(key -> sorted.put(key, properties.getProperty(key)));
        return sorted;
    }

    private static Path bundleFile(Path root, String locale) {
        return root.resolve("modules/common/src/main/resources/i18n/messages_" + locale + ".properties");
    }

    /**
     * 统计文件中声明的条目数（用于发现重复 key：Properties 会静默覆盖重复项）
     */
    private static long declaredEntryCount(Path file) throws IOException {
        try (Stream<String> lines = Files.lines(file, StandardCharsets.UTF_8)) {
            return lines.filter(line -> {
                String text = line.trim();
                return !text.isEmpty() && !text.startsWith("#") && !text.startsWith("!") && text.contains("=");
            }).count();
        }
    }

    private static Set<String> scanReferencedKeys(Path root) throws IOException {
        Set<String> keys = new LinkedHashSet<>();
        // 用 walkFileTree 直接跳过 target/.tmp/node_modules 等目录，避免 CI 里遍历几十万文件
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (!dir.equals(root) && SKIP_DIRECTORIES.contains(dir.getFileName().toString())) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String path = file.toString();
                if (!path.endsWith(".java") || !path.contains("/src/main/java/")) {
                    return FileVisitResult.CONTINUE;
                }
                String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                Matcher matcher = KEY_LITERAL.matcher(content);
                while (matcher.find()) {
                    keys.add(matcher.group(1));
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return keys;
    }

    private static Path locateRepositoryRoot() {
        Path current = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 6 && current != null; i++) {
            if (Files.isDirectory(current.resolve("modules"))
                && Files.exists(bundleFile(current, "zh_CN"))) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }

    private static String preview(Set<String> keys) {
        if (keys.isEmpty()) {
            return "[]";
        }
        return keys.stream().limit(10).collect(Collectors.joining("、", "[", keys.size() > 10 ? " ...]" : "]"));
    }
}
