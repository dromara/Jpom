/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.configuration;

import cn.hutool.core.util.ReUtil;
import org.junit.Assert;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * 插件端默认配置 {@code file-backup-suffix} 测试。
 *
 * <p>历史问题：YAML 单引号不会处理转义，配置中写成双反斜杠时 Java 实际拿到的是双反斜杠正则，
 * 实际匹配“反斜杠+任意字符”而不是 {@code .txt}，导致默认安装的 Java 插件端不备份 txt 文件。
 *
 * @author Jpom
 * @since 2.11.13.2
 */
public class AgentDefaultBackupSuffixTest {

    private static final String TXT_BACKUP_SUFFIX = "^.+\\.(?i)(txt)$";

    @SuppressWarnings("unchecked")
    private static List<String> loadBackupSuffix(String resource) throws Exception {
        try (InputStream inputStream = AgentDefaultBackupSuffixTest.class.getClassLoader().getResourceAsStream(resource)) {
            Assert.assertNotNull("缺少配置文件: " + resource, inputStream);
            Map<String, Object> root = new Yaml().load(inputStream);
            Map<String, Object> jpom = (Map<String, Object>) root.get("jpom");
            Map<String, Object> project = (Map<String, Object>) jpom.get("project");
            return (List<String>) project.get("file-backup-suffix");
        }
    }

    @Test
    public void testDefaultBackupSuffixBacksUpTxt() throws Exception {
        // config_default 是发布包 conf 的来源（modules/agent/src/main/assembly/release.xml），application.yml 为 IDE 调试默认值
        for (String resource : new String[]{"config_default/application.yml", "application.yml"}) {
            List<String> backupSuffix = loadBackupSuffix(resource);
            Assert.assertNotNull(resource, backupSuffix);
            Assert.assertTrue(resource + " 缺少 txt 备份后缀", backupSuffix.contains(TXT_BACKUP_SUFFIX));
            Assert.assertTrue(resource + " 的 txt 后缀正则无法匹配 .txt 文件",
                ReUtil.isMatch(TXT_BACKUP_SUFFIX, "app.txt"));
            // 双反斜杠是历史错误写法，必须无法匹配
            Assert.assertFalse(resource + " 仍使用历史双反斜杠写法",
                ReUtil.isMatch("^.+\\\\.(?i)(txt)$", "app.txt"));
        }
    }
}
