/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service.sshproject;

import cn.hutool.core.io.FileUtil;
import cn.hutool.extra.spring.SpringUtil;
import org.dromara.jpom.model.data.DslYmlDto;
import org.dromara.jpom.model.sshproject.SshProjectTemplate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 模板渲染的行为级测试：
 * <ul>
 *     <li>渲染结果必须能被项目配置解析（DslYmlDto.build 不抛错）；</li>
 *     <li>必填字段缺失、字段含 shell 元字符必须拒绝；</li>
 *     <li>渲染出的脚本必须是合法 bash（bash -n）。</li>
 * </ul>
 *
 * @author bwcx_jzy
 * @since 2026/11/20
 */
public class SshProjectTemplateServiceTest {

    private final SshProjectTemplateService service = new SshProjectTemplateService();

    /**
     * 模板校验失败时的提示走 I18nMessageUtil，需要一个可用的 MessageSource
     */
    @BeforeAll
    public static void initMessageSource() {
        GenericApplicationContext context = new GenericApplicationContext();
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("i18n/messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setUseCodeAsDefaultMessage(true);
        context.getBeanFactory().registerSingleton("messageSource", messageSource);
        context.refresh();
        new SpringUtil().setApplicationContext(context);
    }

    @Test
    public void listContainsBuiltInTemplates() {
        List<SshProjectTemplate> list = service.list();
        Assertions.assertFalse(list.isEmpty());
        Assertions.assertTrue(list.stream().anyMatch(template -> "spring-boot-jar".equals(template.getKey())));
        Assertions.assertTrue(list.stream().anyMatch(template -> "file-only".equals(template.getKey())));
        Assertions.assertTrue(list.stream().anyMatch(template -> "docker-compose".equals(template.getKey())));
        Assertions.assertTrue(list.stream().anyMatch(template -> "systemd".equals(template.getKey())));
        Assertions.assertTrue(list.stream().anyMatch(template -> "java-classpath".equals(template.getKey())));
        Assertions.assertTrue(list.stream().anyMatch(template -> "nodejs".equals(template.getKey())));
        Assertions.assertTrue(list.stream().anyMatch(template -> "python".equals(template.getKey())));
        Assertions.assertTrue(list.stream().anyMatch(template -> "go-binary".equals(template.getKey())));
        Assertions.assertTrue(list.stream().anyMatch(template -> "nginx-static".equals(template.getKey())));
        Assertions.assertEquals(9, list.size(), "首批 9 个内置模板");
    }

    @Test
    public void renderSpringBootJarIsValidProjectConfig() {
        String yml = service.render("spring-boot-jar", "{\"javaBin\":\"/usr/local/jdk8/bin/java\"}");
        DslYmlDto dslYmlDto = DslYmlDto.build(yml);
        Assertions.assertNotNull(dslYmlDto.getRun());
        Assertions.assertNotNull(dslYmlDto.getRun().getStart());
        Assertions.assertTrue(dslYmlDto.getRun().getStart().getScript().contains("-DJpom.application.ssh="));
        Assertions.assertTrue(dslYmlDto.getRun().getStart().getScript().contains("/usr/local/jdk8/bin/java"));
        Assertions.assertNotNull(dslYmlDto.getRun().getStatus());
        Assertions.assertNotNull(dslYmlDto.getRun().getStop());
        Assertions.assertNotNull(dslYmlDto.getConfig());
        Assertions.assertEquals("logs/app.log", dslYmlDto.getConfig().getLog().get(0).getPath());
    }

    @Test
    public void renderFileOnlyHasNoHooks() {
        String yml = service.render("file-only", "{}");
        DslYmlDto dslYmlDto = DslYmlDto.build(yml);
        Assertions.assertNull(dslYmlDto.getRun());
    }

    @Test
    public void renderRejectsMissingRequiredField() {
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
            () -> service.render("systemd", "{}"));
        Assertions.assertTrue(exception.getMessage().contains("unit"));
    }

    @Test
    public void renderRejectsShellMetaCharacters() {
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> service.render("systemd", "{\"unitName\":\"a.service; rm -rf /\"}"));
    }

    @Test
    public void renderedScriptsAreValidBash() throws Exception {
        for (SshProjectTemplate template : service.list()) {
            String fields = this.sampleFields(template);
            String yml = service.render(template.getKey(), fields);
            DslYmlDto dslYmlDto = DslYmlDto.build(yml);
            if (dslYmlDto.getRun() == null) {
                continue;
            }
            Map<String, String> scripts = new java.util.LinkedHashMap<>();
            scripts.put("start", dslYmlDto.getRun().getStart() == null ? null : dslYmlDto.getRun().getStart().getScript());
            scripts.put("status", dslYmlDto.getRun().getStatus() == null ? null : dslYmlDto.getRun().getStatus().getScript());
            scripts.put("stop", dslYmlDto.getRun().getStop() == null ? null : dslYmlDto.getRun().getStop().getScript());
            for (Map.Entry<String, String> entry : scripts.entrySet()) {
                if (entry.getValue() == null) {
                    continue;
                }
                File file = FileUtil.createTempFile("jpom-ssh-project-" + template.getKey() + "-" + entry.getKey(), ".sh", true);
                FileUtil.writeUtf8String(entry.getValue(), file);
                Process process = new ProcessBuilder("bash", "-n", file.getAbsolutePath())
                    .redirectErrorStream(true)
                    .start();
                String output = cn.hutool.core.io.IoUtil.readUtf8(process.getInputStream());
                int exit = process.waitFor();
                Assertions.assertEquals(0, exit, template.getKey() + "/" + entry.getKey() + " 不是合法 bash: " + output);
            }
        }
    }

    /**
     * 给每个模板填一份满足必填项的样例值
     */
    private String sampleFields(SshProjectTemplate template) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("mainClass", "com.example.Main");
        values.put("libDir", "lib");
        values.put("entry", "index.js");
        values.put("binPath", "./app");
        values.put("composeFile", "docker-compose.yml");
        values.put("unitName", "my-app.service");
        values.put("confPath", "/etc/nginx/nginx.conf");
        if ("python".equals(template.getKey())) {
            values.put("entry", "-m uvicorn app:app --port 8000");
        }
        return com.alibaba.fastjson2.JSON.toJSONString(values);
    }

    @Test
    public void parseProjectDslWithConfigLog() {
        String yml = "description: r2\n"
            + "run:\n"
            + "  start:\n"
            + "    script: |\n"
            + "      echo start\n"
            + "config:\n"
            + "  log:\n"
            + "    - name: 应用日志\n"
            + "      path: logs/app.log\n";
        DslYmlDto dsl = DslYmlDto.build(yml);
        Assertions.assertNotNull(dsl.getRun());
        Assertions.assertEquals("echo start\n", dsl.getRun().getStart().getScript());
        Assertions.assertEquals("logs/app.log", dsl.getConfig().getLog().get(0).getPath());
    }
}
