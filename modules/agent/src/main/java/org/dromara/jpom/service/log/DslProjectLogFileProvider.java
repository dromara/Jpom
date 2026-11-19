/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service.log;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import org.dromara.jpom.model.RunMode;
import org.dromara.jpom.model.data.DslYmlDto;
import org.dromara.jpom.model.data.NodeProjectInfoModel;
import org.dromara.jpom.model.data.ProjectLogFileItem;
import org.dromara.jpom.service.manage.ProjectInfoService;
import org.dromara.jpom.util.FileUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DSL 项目的运行日志文件提供者
 * <p>
 * 读取 DSL 配置中的 `config.log` 列表（见 {@link DslYmlDto.Config#getLog()}），把配置的路径解析为
 * 实际的日志文件，供项目控制台直接选择查看。
 *
 * @author bwcx_jzy
 * @since 2026/11/19
 */
@Service
public class DslProjectLogFileProvider implements ProjectLogFileProvider {

    private final ProjectInfoService projectInfoService;

    public DslProjectLogFileProvider(ProjectInfoService projectInfoService) {
        this.projectInfoService = projectInfoService;
    }

    @Override
    public RunMode runMode() {
        return RunMode.Dsl;
    }

    @Override
    public List<ProjectLogFileItem> list(NodeProjectInfoModel nodeProjectInfoModel) {
        NodeProjectInfoModel originalModel = projectInfoService.resolveModel(nodeProjectInfoModel);
        DslYmlDto dslYmlDto = originalModel.dslConfig();
        if (dslYmlDto == null) {
            return Collections.emptyList();
        }
        DslYmlDto.Config config = dslYmlDto.getConfig();
        if (config == null || CollUtil.isEmpty(config.getLog())) {
            return Collections.emptyList();
        }
        File libDir = projectInfoService.resolveLibFile(nodeProjectInfoModel);
        Map<String, String> variables = this.variables(nodeProjectInfoModel);
        Charset defaultCharset = projectInfoService.resolveLogCharset(nodeProjectInfoModel, originalModel);
        List<ProjectLogFileItem> result = new ArrayList<>();
        for (DslYmlDto.LogFile logFile : config.getLog()) {
            ProjectLogFileItem item = build(libDir, logFile, variables, defaultCharset);
            if (item == null) {
                continue;
            }
            result.add(item);
        }
        return result;
    }

    @Override
    public ProjectLogFileItem resolve(NodeProjectInfoModel nodeProjectInfoModel, String key) {
        if (StrUtil.isEmpty(key)) {
            return null;
        }
        for (ProjectLogFileItem item : this.list(nodeProjectInfoModel)) {
            if (StrUtil.equals(item.getKey(), key)) {
                return item;
            }
        }
        return null;
    }

    /**
     * 构建单个日志文件信息
     *
     * @param libDir        项目路径
     * @param logFile       DSL 日志配置
     * @param variables     路径变量
     * @param defaultCharset 默认编码
     * @return 日志文件信息，路径非法返回 null
     */
    private static ProjectLogFileItem build(File libDir,
                                            DslYmlDto.LogFile logFile,
                                            Map<String, String> variables,
                                            Charset defaultCharset) {
        if (logFile == null) {
            return null;
        }
        File file = resolveFile(libDir, logFile.getPath(), variables);
        if (file == null) {
            return null;
        }
        ProjectLogFileItem item = new ProjectLogFileItem();
        item.setPath(FileUtil.getAbsolutePath(file));
        String name = StrUtil.emptyToDefault(StrUtil.trim(logFile.getName()), file.getName());
        item.setName(name);
        // 标识用配置内容的摘要：中文名称经过表单转发可能被转码，标识只使用 ASCII
        item.setKey(logKey(logFile));
        Charset charset = CharsetUtil.parse(logFile.getCharset(), defaultCharset);
        item.setCharset(charset.name());
        boolean exists = FileUtil.isFile(file);
        item.setExists(exists);
        if (exists) {
            long size = file.length();
            item.setSize(size);
            item.setSizeText(FileUtil.readableFileSize(size));
        } else {
            item.setSize(0L);
        }
        return item;
    }

    /**
     * 日志标识：由配置的 name + path 计算，保证 Java/Go 两端一致且只含 ASCII
     *
     * @param logFile 日志配置
     * @return 标识
     */
    public static String logKey(DslYmlDto.LogFile logFile) {
        String source = StrUtil.nullToEmpty(logFile.getName()) + "\n" + StrUtil.nullToEmpty(logFile.getPath());
        return SecureUtil.md5(source);
    }

    /**
     * 解析日志文件路径
     * <p>
     * 支持 ${PROJECT_PATH}、${PROJECT_ID}、${PROJECT_NAME} 变量；绝对路径直接使用，
     * 相对路径限制在项目路径内（避免配置出现 .. 越界）。
     *
     * @param libDir    项目路径
     * @param path      配置的路径
     * @param variables 变量
     * @return file，非法路径返回 null
     */
    public static File resolveFile(File libDir, String path, Map<String, String> variables) {
        String resolved = StrUtil.trim(path);
        if (StrUtil.isEmpty(resolved)) {
            return null;
        }
        if (MapUtil.isNotEmpty(variables)) {
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                resolved = StrUtil.replace(resolved, "${" + entry.getKey() + "}", StrUtil.nullToEmpty(entry.getValue()));
            }
        }
        resolved = StrUtil.trim(resolved);
        if (StrUtil.isEmpty(resolved)) {
            return null;
        }
        if (FileUtil.isAbsolutePath(resolved)) {
            return FileUtil.file(resolved);
        }
        if (libDir == null) {
            return null;
        }
        return FileUtils.resolveSubFile(libDir, resolved);
    }

    /**
     * DSL 路径中可用的变量
     *
     * @param nodeProjectInfoModel 项目
     * @return 变量
     */
    private Map<String, String> variables(NodeProjectInfoModel nodeProjectInfoModel) {
        Map<String, String> variables = new HashMap<>(4);
        variables.put("PROJECT_ID", StrUtil.nullToEmpty(nodeProjectInfoModel.getId()));
        variables.put("PROJECT_NAME", StrUtil.nullToEmpty(nodeProjectInfoModel.getName()));
        try {
            variables.put("PROJECT_PATH", projectInfoService.resolveLibPath(nodeProjectInfoModel));
        } catch (Exception ignored) {
            // 未配置授权目录时不提供 PROJECT_PATH 变量
        }
        return variables;
    }
}
