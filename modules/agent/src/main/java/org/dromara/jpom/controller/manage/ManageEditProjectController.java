/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.controller.manage;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.RegexPool;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.BaseAgentController;
import org.dromara.jpom.common.Const;
import org.dromara.jpom.common.commander.ProjectCommander;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.model.RunMode;
import org.dromara.jpom.model.data.DslYmlDto;
import org.dromara.jpom.model.data.NodeProjectInfoModel;
import org.dromara.jpom.service.WhitelistDirectoryService;
import org.dromara.jpom.socket.ConsoleCommandOp;
import org.dromara.jpom.util.CommandUtil;
import org.dromara.jpom.util.FileUtils;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.List;

/**
 * 编辑项目
 *
 * @author bwcx_jzy
 * @since 2019/4/16
 */
@RestController
@RequestMapping(value = "/manage/")
@Slf4j
public class ManageEditProjectController extends BaseAgentController {

    private final WhitelistDirectoryService whitelistDirectoryService;
    private final ProjectCommander projectCommander;

    public ManageEditProjectController(WhitelistDirectoryService whitelistDirectoryService,
                                       ProjectCommander projectCommander) {
        this.whitelistDirectoryService = whitelistDirectoryService;
        this.projectCommander = projectCommander;
    }

    /**
     * 基础检查
     *
     * @param projectInfo 项目实体
     * @param previewData 预检查数据
     */
    private void checkParameter(NodeProjectInfoModel projectInfo, boolean previewData) {
        String id = projectInfo.getId();
        // 兼容 _
        String checkId = StrUtil.replace(id, StrUtil.DASHED, StrUtil.UNDERLINE);
        Validator.validateGeneral(checkId, 2, Const.ID_MAX_LEN, I18nMessageUtil.get("i18n.project_id_length_range.7064"));
        Assert.state(!Const.SYSTEM_ID.equals(id), StrUtil.format(I18nMessageUtil.get("i18n.project_id_keyword_occupied.1cae"), Const.SYSTEM_ID));
        // 运行模式
        RunMode runMode = projectInfo.getRunMode();
        Assert.notNull(runMode, I18nMessageUtil.get("i18n.select_run_mode.5a5d"));
        // 监测
        if (runMode == RunMode.ClassPath || runMode == RunMode.JavaExtDirsCp) {
            Assert.hasText(projectInfo.mainClass(), I18nMessageUtil.get("i18n.class_path_and_java_ext_dirs_cp_required.7557"));
            if (runMode == RunMode.JavaExtDirsCp) {
                Assert.hasText(projectInfo.javaExtDirsCp(), I18nMessageUtil.get("i18n.java_ext_dirs_cp_required.1f4a"));
            }
        } else if (runMode == RunMode.Jar || runMode == RunMode.JarWar) {
            projectInfo.setMainClass(StrUtil.EMPTY);
        } else if (runMode == RunMode.Link) {
            String linkId = projectInfo.getLinkId();
            Assert.hasText(linkId, I18nMessageUtil.get("i18n.link_id_required.5dc7"));
            NodeProjectInfoModel item = projectInfoService.getItem(linkId);
            Assert.notNull(item, I18nMessageUtil.get("i18n.soft_link_project_department_exists.fa97"));
            RunMode itemRunMode = item.getRunMode();
            Assert.state(itemRunMode != RunMode.File && itemRunMode != RunMode.Link, I18nMessageUtil.get("i18n.soft_link_project_mode_error.ffa0"));
        }
        // 判断是否为分发添加
        String strOutGivingProject = getParameter("outGivingProject");
        boolean outGivingProject = Boolean.parseBoolean(strOutGivingProject);

        projectInfo.setOutGivingProject(outGivingProject);
        if (runMode != RunMode.Link) {
            if (!previewData) {
                String whitelistDirectory = projectInfo.whitelistDirectory();
                // 不是预检查数据才效验授权
                if (!whitelistDirectoryService.checkProjectDirectory(whitelistDirectory)) {
                    if (outGivingProject) {
                        whitelistDirectoryService.addProjectWhiteList(whitelistDirectory);
                    } else {
                        throw new IllegalArgumentException(I18nMessageUtil.get("i18n.select_correct_project_path_or_no_auth_configured.366a"));
                    }
                }
                String logPath = projectInfo.logPath();
                if (StrUtil.isNotEmpty(logPath)) {
                    if (!whitelistDirectoryService.checkProjectDirectory(logPath)) {
                        if (outGivingProject) {
                            whitelistDirectoryService.addProjectWhiteList(logPath);
                        } else {
                            throw new IllegalArgumentException(I18nMessageUtil.get("i18n.project_log_storage_path_required.d0bb"));
                        }
                    }
                }
            }
            //
            String lib = projectInfo.getLib();
            Assert.state(StrUtil.isNotEmpty(lib) && !StrUtil.SLASH.equals(lib) && !Validator.isChinese(lib), I18nMessageUtil.get("i18n.invalid_project_path.04f7"));

            FileUtils.checkSlip(lib, e -> new IllegalArgumentException(I18nMessageUtil.get("i18n.project_path_promotion_issue.2250")));
        }
    }


    @RequestMapping(value = "saveProject", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<String> saveProject(NodeProjectInfoModel projectInfo) {
        // 预检查数据
        String strPreviewData = getParameter("previewData");
        boolean previewData = Convert.toBool(strPreviewData, false);

        //
        this.checkParameter(projectInfo, previewData);

        // 自动生成log文件
        File checkFile = this.projectInfoService.resolveAbsoluteLogFile(projectInfo);
        Assert.state(!FileUtil.exist(checkFile) || FileUtil.isFile(checkFile), I18nMessageUtil.get("i18n.project_log_is_existing_folder.a80a"));
        //
        String token = projectInfo.token();
        if (StrUtil.isNotEmpty(token)) {
            Validator.validateMatchRegex(RegexPool.URL_HTTP, token, I18nMessageUtil.get("i18n.invalid_webhooks_address.d836"));
        }
        // 判断 yml
        this.checkDslYml(projectInfo);
        //
        return this.save(projectInfo, previewData);
    }

    private void checkDslYml(NodeProjectInfoModel projectInfo) {
        if (projectInfo.getRunMode() == RunMode.Dsl) {
            String dslContent = projectInfo.getDslContent();
            Assert.hasText(dslContent, I18nMessageUtil.get("i18n.configure_dsl_content.42e3"));
            DslYmlDto build = DslYmlDto.build(dslContent);
            Assert.state(build.hasRunProcess(ConsoleCommandOp.status.name()), I18nMessageUtil.get("i18n.run_status_not_configured.e959"));
        }
    }

    /**
     * 保存项目
     *
     * @param projectInfo 项目
     * @param previewData 是否是预检查
     * @return 错误信息
     */
    private IJsonMessage<String> save(NodeProjectInfoModel projectInfo, boolean previewData) {
        this.checkPath(projectInfo);
        //
        NodeProjectInfoModel exits = projectInfoService.getItem(projectInfo.getId());
        String workspaceId = this.getWorkspaceId();
        projectInfo.setWorkspaceId(workspaceId);
        RunMode runMode = projectInfo.getRunMode();
        if (exits == null) {
            // 检查运行中的tag 是否被占用
            if (runMode != RunMode.File && runMode != RunMode.Dsl) {
                Assert.state(!projectCommander.isRun(projectInfo), I18nMessageUtil.get("i18n.project_id_in_use.1adb"));
            }
            if (previewData) {
                // 预检查数据
                return JsonMessage.success(I18nMessageUtil.get("i18n.check_passed.dce8"));
            } else {
                projectInfoService.addItem(projectInfo);
                return JsonMessage.success(I18nMessageUtil.get("i18n.add_new_success.431a"));
            }
        }
        if (previewData) {
            // 预检查数据
            return JsonMessage.success(I18nMessageUtil.get("i18n.check_passed.dce8"));
        } else {
            exits.setNodeId(projectInfo.getNodeId());
            exits.setName(projectInfo.getName());
            exits.setGroup(projectInfo.getGroup());
            exits.setAutoStart(projectInfo.getAutoStart());
            exits.setDisableScanDir(projectInfo.getDisableScanDir());
            exits.setMainClass(projectInfo.mainClass());
            exits.setJvm(projectInfo.getJvm());
            exits.setArgs(projectInfo.getArgs());
            if (StrUtil.isNotEmpty(exits.getWorkspaceId())) {
                Assert.state(StrUtil.equals(exits.getWorkspaceId(), workspaceId), I18nMessageUtil.get("i18n.project_associated_with_other_workspaces_message.299c"));
            }
            exits.setWorkspaceId(workspaceId);
            exits.setOutGivingProject(projectInfo.outGivingProject());
            exits.setRunMode(runMode);
            exits.setToken(projectInfo.token());
            exits.setDslContent(projectInfo.getDslContent());
            exits.setDslEnv(projectInfo.getDslEnv());
            exits.setJavaExtDirsCp(projectInfo.javaExtDirsCp());
            if (runMode == RunMode.Link) {
                // 如果是链接模式
                Assert.state(runMode == exits.getRunMode(), I18nMessageUtil.get("i18n.existing_project_cannot_be_soft_link.aa5a"));
            } else {
                // 移动到新路径
                this.moveTo(exits, projectInfo);
                // 最后才设置新的路径
                exits.setLib(projectInfo.getLib());
                exits.setWhitelistDirectory(projectInfo.whitelistDirectory());
                //
                //exits.setLog(projectInfo.log());
                exits.setLogPath(projectInfo.logPath());
            }
            projectInfoService.updateById(exits, exits.getId());
            return JsonMessage.success(I18nMessageUtil.get("i18n.modify_success.69be"));
        }
    }

    private void moveTo(NodeProjectInfoModel old, NodeProjectInfoModel news) {
        {
            // 移动目录
            File oldLib = projectInfoService.resolveLibFile(old);
            File newLib = projectInfoService.resolveLibFile(news);
            if (!FileUtil.equals(oldLib, newLib)) {
                // 正在运行的项目不能修改路径
                this.projectMustNotRun(old, I18nMessageUtil.get("i18n.running_project_cannot_change_path.5888"));
                if (oldLib.exists()) {
                    FileUtils.tempMoveContent(oldLib, newLib);
                }
            }
        }
        {
            // log
            File oldLog = projectInfoService.resolveAbsoluteLogFile(old);
            File newLog = projectInfoService.resolveAbsoluteLogFile(news);
            if (!FileUtil.equals(oldLog, newLog)) {
                // 正在运行的项目不能修改路径
                this.projectMustNotRun(old, I18nMessageUtil.get("i18n.running_project_cannot_change_path.5888"));
                if (oldLog.exists()) {
                    FileUtil.mkParentDirs(newLog);
                    FileUtil.move(oldLog, newLog, true);
                }
                // logBack
                File oldLogBack = projectInfoService.resolveLogBack(old);
                if (oldLogBack.exists()) {
                    File logBack = projectInfoService.resolveLogBack(news);
                    FileUtils.tempMoveContent(oldLogBack, logBack);
                }
            }
        }
    }

    private void projectMustNotRun(NodeProjectInfoModel projectInfoModel, String msg) {
        boolean run = projectCommander.isRun(projectInfoModel);
        Assert.state(!run, msg);
    }

    /**
     * 路径存在包含关系
     *
     * @param nodeProjectInfoModel 比较的项目
     */
    private void checkPath(NodeProjectInfoModel nodeProjectInfoModel) {
        String id = nodeProjectInfoModel.getId();
        Assert.state(!id.contains(StrUtil.SPACE), I18nMessageUtil.get("i18n.project_id_cannot_contain_spaces.251d"));
        RunMode runMode = nodeProjectInfoModel.getRunMode();
        if (runMode == RunMode.Link) {
            return;
        }
        List<NodeProjectInfoModel> list = projectInfoService.list();
        if (list == null) {
            return;
        }
        //
        String allLib = nodeProjectInfoModel.allLib();
        // 判断空格
        Assert.state(!allLib.contains(StrUtil.SPACE), I18nMessageUtil.get("i18n.project_path_no_spaces.263c"));
        File checkFile = FileUtil.file(allLib);
        Assert.state(!FileUtil.exist(checkFile) || FileUtil.isDirectory(checkFile), I18nMessageUtil.get("i18n.project_path_already_exists_as_file.a900"));
        // 重复lib
        for (NodeProjectInfoModel item : list) {
            if (item.getRunMode() == RunMode.Link) {
                continue;
            }
            File fileLib = projectInfoService.resolveLibFile(item);
            if (!nodeProjectInfoModel.getId().equals(id) && FileUtil.equals(fileLib, checkFile)) {
                throw new IllegalArgumentException(StrUtil.format(I18nMessageUtil.get("i18n.project_path_occupied.cddd"), item.getName()));
            }
        }

        NodeProjectInfoModel nodeProjectInfoModel1 = null;
        for (NodeProjectInfoModel model : list) {
            if (model.getRunMode() == RunMode.Link) {
                continue;
            }
            if (!model.getId().equals(nodeProjectInfoModel.getId())) {
                File file1 = projectInfoService.resolveLibFile(model);
                File file2 = projectInfoService.resolveLibFile(nodeProjectInfoModel);
                if (FileUtil.pathEquals(file1, file2)) {
                    nodeProjectInfoModel1 = model;
                    break;
                }
                // 包含关系
                if (FileUtil.isSub(file1, file2) || FileUtil.isSub(file2, file1)) {
                    nodeProjectInfoModel1 = model;
                    break;
                }
            }
        }
        if (nodeProjectInfoModel1 != null) {
            throw new IllegalArgumentException(StrUtil.format(I18nMessageUtil.get("i18n.project_path_conflict.8c6f"), nodeProjectInfoModel1.getName(), nodeProjectInfoModel1.allLib()));
        }
    }

    /**
     * 删除项目
     *
     * @return json
     */
    @RequestMapping(value = "deleteProject", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<String> deleteProject(String id, String thorough) {
        NodeProjectInfoModel nodeProjectInfoModel = tryGetProjectInfoModel();
        if (nodeProjectInfoModel == null) {
            // 返回正常 200 状态码，考虑节点分发重复操作
            return JsonMessage.success(I18nMessageUtil.get("i18n.project_does_not_exist.3029"));
        }
        // 运行判断
        boolean run = projectCommander.isRun(nodeProjectInfoModel);
        Assert.state(!run, I18nMessageUtil.get("i18n.cannot_delete_running_project.e56b"));
        // 判断是否被软链
        List<NodeProjectInfoModel> list = projectInfoService.list();
        for (NodeProjectInfoModel projectInfoModel : list) {
            if (nodeProjectInfoModel.getRunMode() != RunMode.Link) {
                continue;
            }
            Assert.state(!StrUtil.equals(projectInfoModel.getLinkId(), id), StrUtil.format(I18nMessageUtil.get("i18n.project_soft_linked_by.8556"), projectInfoModel.getName()));
        }
        this.thorough(thorough, nodeProjectInfoModel);
        //
        projectInfoService.deleteItem(nodeProjectInfoModel.getId());

        return JsonMessage.success(I18nMessageUtil.get("i18n.deletion_success_message.4359"));

    }

    /**
     * 彻底删除项目文件
     *
     * @param thorough             是否彻底
     * @param nodeProjectInfoModel 项目
     */
    private void thorough(String thorough, NodeProjectInfoModel nodeProjectInfoModel) {
        if (StrUtil.isEmpty(thorough)) {
            return;
        }
        File logBack = this.projectInfoService.resolveLogBack(nodeProjectInfoModel);
        boolean fastDel = CommandUtil.systemFastDel(logBack);
        Assert.state(!fastDel, I18nMessageUtil.get("i18n.delete_log_file_failure_with_colon.d867") + logBack.getAbsolutePath());
        File log = this.projectInfoService.resolveAbsoluteLogFile(nodeProjectInfoModel);
        fastDel = CommandUtil.systemFastDel(log);
        Assert.state(!fastDel, I18nMessageUtil.get("i18n.delete_log_file_failure_with_colon.d867") + log.getAbsolutePath());
        //
        if (nodeProjectInfoModel.getRunMode() != RunMode.Link) {
            // 非软链项目才删除文件
            File allLib = projectInfoService.resolveLibFile(nodeProjectInfoModel);
            fastDel = CommandUtil.systemFastDel(allLib);
            Assert.state(!fastDel, I18nMessageUtil.get("i18n.delete_project_file_failure.f007") + allLib.getAbsolutePath());
        }
    }

    @RequestMapping(value = "releaseOutGiving", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<Object> releaseOutGiving() {
        NodeProjectInfoModel nodeProjectInfoModel = tryGetProjectInfoModel();
        if (nodeProjectInfoModel != null) {
            NodeProjectInfoModel update = new NodeProjectInfoModel();
            update.setOutGivingProject(false);
            projectInfoService.updateById(update, nodeProjectInfoModel.getId());
        }
        return JsonMessage.success(I18nMessageUtil.get("i18n.release_successful.f2ca"));
    }

    @RequestMapping(value = "change-workspace-id", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<Object> changeWorkspaceId(String newWorkspaceId, String newNodeId) {
        Assert.hasText(newWorkspaceId, I18nMessageUtil.get("i18n.select_workspace_to_modify.ac87"));
        Assert.hasText(newWorkspaceId, I18nMessageUtil.get("i18n.select_node_to_modify.6617"));
        NodeProjectInfoModel nodeProjectInfoModel = tryGetProjectInfoModel();
        if (nodeProjectInfoModel != null) {
            NodeProjectInfoModel update = new NodeProjectInfoModel();
            update.setNodeId(newNodeId);
            update.setWorkspaceId(newWorkspaceId);
            projectInfoService.updateById(update, nodeProjectInfoModel.getId());
        }
        return JsonMessage.success(I18nMessageUtil.get("i18n.modify_success.69be"));
    }
}
