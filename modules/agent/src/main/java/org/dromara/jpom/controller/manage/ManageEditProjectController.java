/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
        Validator.validateGeneral(checkId, 2, Const.ID_MAX_LEN, "项目id 长度范围2-20（英文字母 、数字和下划线）");
        Assert.state(!Const.SYSTEM_ID.equals(id), "项目id " + Const.SYSTEM_ID + " 关键词被系统占用");
        // 运行模式
        RunMode runMode = projectInfo.getRunMode();
        Assert.notNull(runMode, "请选择运行模式");
        // 监测
        if (runMode == RunMode.ClassPath || runMode == RunMode.JavaExtDirsCp) {
            Assert.hasText(projectInfo.mainClass(), "ClassPath、JavaExtDirsCp 模式 MainClass必填");
            if (runMode == RunMode.JavaExtDirsCp) {
                Assert.hasText(projectInfo.javaExtDirsCp(), "JavaExtDirsCp 模式 javaExtDirsCp必填");
            }
        } else if (runMode == RunMode.Jar || runMode == RunMode.JarWar) {
            projectInfo.setMainClass(StrUtil.EMPTY);
        } else if (runMode == RunMode.Link) {
            String linkId = projectInfo.getLinkId();
            Assert.hasText(linkId, "Link 模式 LinkId必填");
            NodeProjectInfoModel item = projectInfoService.getItem(linkId);
            Assert.notNull(item, "软链的项目部存在");
            RunMode itemRunMode = item.getRunMode();
            Assert.state(itemRunMode != RunMode.File && itemRunMode != RunMode.Link, "被软链的项目不能是File或Link模式");
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
                        throw new IllegalArgumentException("请选择正确的项目路径,或者还没有配置授权");
                    }
                }
                String logPath = projectInfo.logPath();
                if (StrUtil.isNotEmpty(logPath)) {
                    if (!whitelistDirectoryService.checkProjectDirectory(logPath)) {
                        if (outGivingProject) {
                            whitelistDirectoryService.addProjectWhiteList(logPath);
                        } else {
                            throw new IllegalArgumentException("请填写的项目日志存储路径,或者还没有配置授权");
                        }
                    }
                }
            }
            //
            String lib = projectInfo.getLib();
            Assert.state(StrUtil.isNotEmpty(lib) && !StrUtil.SLASH.equals(lib) && !Validator.isChinese(lib), "项目路径不能为空,不能为顶级目录,不能包含中文");

            FileUtils.checkSlip(lib, e -> new IllegalArgumentException("项目路径存在提升目录问题"));
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
        Assert.state(!FileUtil.exist(checkFile) || FileUtil.isFile(checkFile), "项目log是一个已经存在的文件夹");
        //
        String token = projectInfo.token();
        if (StrUtil.isNotEmpty(token)) {
            Validator.validateMatchRegex(RegexPool.URL_HTTP, token, "WebHooks 地址不合法");
        }
        // 判断 yml
        this.checkDslYml(projectInfo);
        //
        return this.save(projectInfo, previewData);
    }

    private void checkDslYml(NodeProjectInfoModel projectInfo) {
        if (projectInfo.getRunMode() == RunMode.Dsl) {
            String dslContent = projectInfo.getDslContent();
            Assert.hasText(dslContent, "请配置 dsl 内容");
            DslYmlDto build = DslYmlDto.build(dslContent);
            Assert.state(build.hasRunProcess(ConsoleCommandOp.status.name()), "没有配置 run.status");
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
        projectInfo.setWorkspaceId(this.getWorkspaceId());
        RunMode runMode = projectInfo.getRunMode();
        if (exits == null) {
            // 检查运行中的tag 是否被占用
            if (runMode != RunMode.File && runMode != RunMode.Dsl) {
                Assert.state(!projectCommander.isRun(projectInfo), "当前项目id已经被正在运行的程序占用");
            }
            if (previewData) {
                // 预检查数据
                return JsonMessage.success("检查通过");
            } else {
                projectInfoService.addItem(projectInfo);
                return JsonMessage.success("新增成功！");
            }
        }
        if (previewData) {
            // 预检查数据
            return JsonMessage.success("检查通过");
        } else {
            exits.setNodeId(projectInfo.getNodeId());
            exits.setName(projectInfo.getName());
            exits.setGroup(projectInfo.getGroup());
            exits.setAutoStart(projectInfo.getAutoStart());
            exits.setMainClass(projectInfo.mainClass());
            exits.setJvm(projectInfo.getJvm());
            exits.setArgs(projectInfo.getArgs());
            exits.setWorkspaceId(this.getWorkspaceId());
            exits.setOutGivingProject(projectInfo.outGivingProject());
            exits.setRunMode(runMode);
            exits.setToken(projectInfo.token());
            exits.setDslContent(projectInfo.getDslContent());
            exits.setDslEnv(projectInfo.getDslEnv());
            exits.setJavaExtDirsCp(projectInfo.javaExtDirsCp());
            if (runMode == RunMode.Link) {
                // 如果是链接模式
                Assert.state(runMode == exits.getRunMode(), "已经存在的项目不能修改为软链项目");
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
            return JsonMessage.success("修改成功");
        }
    }

    private void moveTo(NodeProjectInfoModel old, NodeProjectInfoModel news) {
        {
            // 移动目录
            File oldLib = projectInfoService.resolveLibFile(old);
            File newLib = projectInfoService.resolveLibFile(news);
            if (!FileUtil.equals(oldLib, newLib)) {
                // 正在运行的项目不能修改路径
                this.projectMustNotRun(old, "正在运行的项目不能修改路径");
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
                this.projectMustNotRun(old, "正在运行的项目不能修改路径");
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
        Assert.state(!id.contains(StrUtil.SPACE), "项目Id不能包含空格");
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
        Assert.state(!allLib.contains(StrUtil.SPACE), "项目路径不能包含空格");
        File checkFile = FileUtil.file(allLib);
        Assert.state(!FileUtil.exist(checkFile) || FileUtil.isDirectory(checkFile), "项目路径是一个已经存在的文件");
        // 重复lib
        for (NodeProjectInfoModel item : list) {
            if (item.getRunMode() == RunMode.Link) {
                continue;
            }
            File fileLib = projectInfoService.resolveLibFile(item);
            if (!nodeProjectInfoModel.getId().equals(id) && FileUtil.equals(fileLib, checkFile)) {
                throw new IllegalArgumentException("当前项目路径已经被【" + item.getName() + "】占用,请检查");
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
            throw new IllegalArgumentException("项目路径和【" + nodeProjectInfoModel1.getName() + "】项目冲突:" + nodeProjectInfoModel1.allLib());
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
            return JsonMessage.success("项目不存在");
        }
        // 运行判断
        boolean run = projectCommander.isRun(nodeProjectInfoModel);
        Assert.state(!run, "不能删除正在运行的项目");
        // 判断是否被软链
        List<NodeProjectInfoModel> list = projectInfoService.list();
        for (NodeProjectInfoModel projectInfoModel : list) {
            if (nodeProjectInfoModel.getRunMode() != RunMode.Link) {
                continue;
            }
            Assert.state(!StrUtil.equals(projectInfoModel.getLinkId(), id), "项目被" + projectInfoModel.getName() + "软链中");
        }
        this.thorough(thorough, nodeProjectInfoModel);
        //
        projectInfoService.deleteItem(nodeProjectInfoModel.getId());

        return JsonMessage.success("删除成功！");

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
        Assert.state(!fastDel, "删除日志文件失败:" + logBack.getAbsolutePath());
        File log = this.projectInfoService.resolveAbsoluteLogFile(nodeProjectInfoModel);
        fastDel = CommandUtil.systemFastDel(log);
        Assert.state(!fastDel, "删除日志文件失败:" + log.getAbsolutePath());
        //
        if (nodeProjectInfoModel.getRunMode() != RunMode.Link) {
            // 非软链项目才删除文件
            File allLib = projectInfoService.resolveLibFile(nodeProjectInfoModel);
            fastDel = CommandUtil.systemFastDel(allLib);
            Assert.state(!fastDel, "删除项目文件失败:" + allLib.getAbsolutePath());
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
        return JsonMessage.success("释放成功");
    }

    @RequestMapping(value = "change-workspace-id", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<Object> changeWorkspaceId(String newWorkspaceId, String newNodeId) {
        Assert.hasText(newWorkspaceId, "请选择要修改的工作空间");
        Assert.hasText(newWorkspaceId, "请选择要修改的节");
        NodeProjectInfoModel nodeProjectInfoModel = tryGetProjectInfoModel();
        if (nodeProjectInfoModel != null) {
            NodeProjectInfoModel update = new NodeProjectInfoModel();
            update.setNodeId(newNodeId);
            update.setWorkspaceId(newWorkspaceId);
            projectInfoService.updateById(update, nodeProjectInfoModel.getId());
        }
        return JsonMessage.success("修改成功");
    }
}
