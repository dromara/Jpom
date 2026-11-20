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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.model.sshproject.SshProjectTemplate;
import org.dromara.jpom.model.sshproject.SshProjectTemplateBundle;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * SSH 项目内置模板：字段表单 → 渲染生命周期 YAML
 * <p>
 * 模板只在创建/编辑时渲染一次，项目保存后与模板解耦，运行期不依赖模板。
 *
 * @author bwcx_jzy
 * @since 2026/11/20
 */
@Service
public class SshProjectTemplateService {

    private static final String TEMPLATE_RESOURCE = "config_default/ssh-project/templates.json";
    /**
     * 模板字段允许的字面量字符（禁止 shell 元字符，避免字段拼进脚本后注入）
     */
    private static final Pattern FIELD_VALUE = Pattern.compile("^[A-Za-z0-9_./:=@%+,\\- ]*$");
    /**
     * 生命周期流程顺序
     */
    private static final String[] HOOK_OPS = new String[]{"start", "status", "stop", "restart", "reload"};

    private volatile List<SshProjectTemplate> cache;

    /**
     * 内置模板列表
     *
     * @return list
     */
    public List<SshProjectTemplate> list() {
        if (this.cache == null) {
            synchronized (this) {
                if (this.cache == null) {
                    this.cache = this.load();
                }
            }
        }
        return this.cache;
    }

    private List<SshProjectTemplate> load() {
        String text = ResourceUtil.readUtf8Str(TEMPLATE_RESOURCE);
        SshProjectTemplateBundle bundle = JSON.parseObject(text, SshProjectTemplateBundle.class);
        Assert.notNull(bundle, I18nMessageUtil.get("i18n.ssh_project_template_not_found.c2a7"));
        List<SshProjectTemplate> templates = bundle.getTemplates();
        Assert.notEmpty(templates, I18nMessageUtil.get("i18n.ssh_project_template_not_found.c2a7"));
        templates.sort((o1, o2) -> Integer.compare(o1.getOrder() == null ? 0 : o1.getOrder(), o2.getOrder() == null ? 0 : o2.getOrder()));
        return templates;
    }

    /**
     * 获取模板
     *
     * @param key key
     * @return template
     */
    public SshProjectTemplate get(String key) {
        return this.list().stream()
            .filter(template -> StrUtil.equals(template.getKey(), key))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(I18nMessageUtil.get("i18n.ssh_project_template_not_found.c2a7") + key));
    }

    /**
     * 渲染模板为生命周期 yml
     *
     * @param key        模板 key
     * @param fieldsJson 字段值（JSON 对象字符串）
     * @return yml
     */
    public String render(String key, String fieldsJson) {
        SshProjectTemplate template = this.get(key);
        JSONObject input = StrUtil.isEmpty(fieldsJson) ? new JSONObject() : JSON.parseObject(fieldsJson);
        Map<String, String> values = this.resolveValues(template, input);
        String yml = this.buildYml(template, values);
        // 自校验：渲染结果必须能被项目配置解析，避免生成半成品
        org.dromara.jpom.model.data.DslYmlDto.build(yml);
        return yml;
    }

    private Map<String, String> resolveValues(SshProjectTemplate template, JSONObject input) {
        Map<String, String> values = new LinkedHashMap<>();
        List<SshProjectTemplate.Field> fields = template.getFields();
        if (CollUtil.isEmpty(fields)) {
            return values;
        }
        for (SshProjectTemplate.Field field : fields) {
            String value = input.getString(field.getKey());
            if (StrUtil.isEmpty(value)) {
                value = field.getDefaultValue();
            }
            value = StrUtil.trimToEmpty(value);
            if (Boolean.TRUE.equals(field.getRequired())) {
                Assert.hasText(value, I18nMessageUtil.get("i18n.ssh_project_template_field_required.d3b8") + field.getLabel());
            }
            if (!FIELD_VALUE.matcher(value).matches()) {
                throw new IllegalArgumentException(I18nMessageUtil.get("i18n.ssh_project_template_field_invalid.e4c9") + field.getLabel() + " [" + value + "]");
            }
            values.put(field.getKey(), value);
        }
        return values;
    }

    private String buildYml(SshProjectTemplate template, Map<String, String> values) {
        StringBuilder builder = new StringBuilder();
        builder.append("description: ").append(this.quote(template.getName())).append("\n");
        Map<String, String> hooks = template.getHooks();
        List<String> ops = new ArrayList<>();
        for (String op : HOOK_OPS) {
            String script = hooks == null ? null : hooks.get(op);
            if (StrUtil.isBlank(script)) {
                continue;
            }
            ops.add(op);
        }
        if (!ops.isEmpty()) {
            builder.append("run:\n");
            for (String op : ops) {
                String script = this.replaceValues(hooks.get(op), values);
                builder.append("  ").append(op).append(":\n");
                builder.append("    script: |\n");
                for (String line : StrUtil.split(script, '\n', true, false)) {
                    builder.append("      ").append(line).append("\n");
                }
            }
        }
        List<SshProjectTemplate.LogFile> logFiles = template.getLogFiles();
        if (CollUtil.isNotEmpty(logFiles)) {
            builder.append("config:\n");
            builder.append("  log:\n");
            for (SshProjectTemplate.LogFile logFile : logFiles) {
                String name = this.replaceValues(logFile.getName(), values);
                String path = this.replaceValues(logFile.getPath(), values);
                builder.append("    - name: ").append(this.quote(name)).append("\n");
                builder.append("      path: ").append(this.quote(path)).append("\n");
            }
        }
        return builder.toString();
    }

    private String replaceValues(String source, Map<String, String> values) {
        if (StrUtil.isEmpty(source)) {
            return StrUtil.EMPTY;
        }
        String result = source;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = StrUtil.replace(result, "{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }

    private String quote(String value) {
        String text = StrUtil.nullToEmpty(value);
        return "\"" + StrUtil.replace(text, "\"", "'") + "\"";
    }

    /**
     * 供测试与前端展示：模板支持的流程
     *
     * @return ops
     */
    public List<String> hookOps() {
        return Arrays.asList(HOOK_OPS);
    }
}
