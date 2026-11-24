/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.controller.sshproject;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.func.assets.controller.BaseSshFileController;
import org.dromara.jpom.func.assets.model.MachineSshModel;
import org.dromara.jpom.model.data.SshProjectModel;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.service.sshproject.SshProjectMachineResolver;
import org.dromara.jpom.service.sshproject.SshProjectService;
import org.dromara.jpom.util.FileUtils;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

/**
 * SSH 项目文件管理
 * <p>
 * 直接复用 SSH 资产文件管理实现（{@link BaseSshFileController}）：
 * 把「项目目录」当作唯一授权目录，路径校验、分片上传、在线编辑等能力全部继承，不重复实现。
 *
 * @author bwcx_jzy
 * @since 2026/11/20
 */
@RestController
@RequestMapping("node/ssh-project/file")
@Feature(cls = ClassFeature.SSH_PROJECT_FILE)
@Slf4j
public class SshProjectFileController extends BaseSshFileController {

    /**
     * 未配置时允许在线编辑的后缀（与 SSH 文件管理默认口径一致，可被资产配置覆盖）
     */
    private static final List<String> DEFAULT_EDIT_SUFFIX = Arrays.asList(
        ".log", ".txt", ".yml", ".yaml", ".conf", ".properties", ".json", ".sh", ".xml", ".ini", ".html", ".css", ".js", ".md");

    @Resource
    private SshProjectService sshProjectService;
    @Resource
    private SshProjectMachineResolver machineResolver;

    @Override
    protected <T> T checkConfigPath(String id, BiFunction<MachineSshModel, ItemConfig, T> function) {
        SshProjectModel project = sshProjectService.getByKey(id, getRequest());
        Assert.notNull(project, I18nMessageUtil.get("i18n.ssh_project_not_exist.5d18"));
        MachineSshModel machine = machineResolver.resolve(project);
        return function.apply(machine, this.itemConfig(project, machine));
    }

    @Override
    protected <T> T checkConfigPathChildren(String id, String path, String children, BiFunction<MachineSshModel, ItemConfig, T> function) {
        FileUtils.checkSlip(path);
        FileUtils.checkRelativePath(children);

        SshProjectModel project = sshProjectService.getByKey(id, getRequest());
        Assert.notNull(project, I18nMessageUtil.get("i18n.ssh_project_not_exist.5d18"));
        MachineSshModel machine = machineResolver.resolve(project);
        ItemConfig itemConfig = this.itemConfig(project, machine);
        // 授权目录统一不带结尾斜杠，前端传回的目录可能带斜杠，比较前先归一化
        String normalize = StrUtil.removeSuffix(FileUtil.normalize(StrUtil.SLASH + path + StrUtil.SLASH), StrUtil.SLASH);
        Assert.state(CollUtil.contains(itemConfig.fileDirs(), normalize), I18nMessageUtil.get("i18n.cannot_operate_current_directory.aa3d"));
        return function.apply(machine, itemConfig);
    }

    private ItemConfig itemConfig(SshProjectModel project, MachineSshModel machine) {
        String path = StrUtil.removeSuffix(StrUtil.nullToEmpty(project.getPath()), "/");
        // 与 SSH 资产的授权目录写法保持一致：不带结尾斜杠（前端目录树直接展示该值）
        List<String> fileDirs = StrUtil.isEmpty(path)
            ? Collections.emptyList()
            : Collections.singletonList(StrUtil.removeSuffix(FileUtil.normalize(StrUtil.SLASH + path), StrUtil.SLASH));
        List<String> editSuffix = cn.hutool.core.util.StrUtil.isEmpty(machine.getAllowEditSuffix())
            ? null
            : com.alibaba.fastjson2.JSON.parseArray(machine.getAllowEditSuffix(), String.class);
        List<String> useSuffix = CollUtil.isEmpty(editSuffix) ? DEFAULT_EDIT_SUFFIX : editSuffix;
        return new ItemConfig() {
            @Override
            public List<String> allowEditSuffix() {
                return useSuffix;
            }

            @Override
            public List<String> fileDirs() {
                return fileDirs;
            }
        };
    }
}
