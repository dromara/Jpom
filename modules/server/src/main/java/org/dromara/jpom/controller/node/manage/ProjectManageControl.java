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
package org.dromara.jpom.controller.node.manage;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.io.CharsetDetector;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.text.StrPool;
import cn.hutool.core.text.csv.*;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.extra.servlet.ServletUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.forward.NodeForward;
import org.dromara.jpom.common.forward.NodeUrl;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.common.validator.ValidatorRule;
import org.dromara.jpom.model.BaseNodeModel;
import org.dromara.jpom.model.PageResultDto;
import org.dromara.jpom.model.RunMode;
import org.dromara.jpom.model.data.BuildInfoModel;
import org.dromara.jpom.model.data.MonitorModel;
import org.dromara.jpom.model.data.NodeModel;
import org.dromara.jpom.model.data.RepositoryModel;
import org.dromara.jpom.model.enums.BuildReleaseMethod;
import org.dromara.jpom.model.node.ProjectInfoCacheModel;
import org.dromara.jpom.permission.*;
import org.dromara.jpom.service.dblog.BuildInfoService;
import org.dromara.jpom.service.dblog.DbBuildHistoryLogService;
import org.dromara.jpom.service.dblog.RepositoryService;
import org.dromara.jpom.service.monitor.MonitorService;
import org.dromara.jpom.service.node.ProjectInfoCacheService;
import org.dromara.jpom.service.outgiving.LogReadServer;
import org.dromara.jpom.service.outgiving.OutGivingServer;
import org.dromara.jpom.service.system.WhitelistDirectoryService;
import org.dromara.jpom.system.ServerConfig;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 项目管理
 *
 * @author Administrator
 * @author bwcx_jzy
 * @since 2018/9/29
 */
@RestController
@RequestMapping(value = "/node/manage/")
@Feature(cls = ClassFeature.PROJECT)
@NodeDataPermission(cls = ProjectInfoCacheService.class)
@Slf4j
public class ProjectManageControl extends BaseServerController {

    private final OutGivingServer outGivingServer;
    private final LogReadServer logReadServer;
    private final MonitorService monitorService;
    private final BuildInfoService buildService;
    private final RepositoryService repositoryService;
    private final ProjectInfoCacheService projectInfoCacheService;
    private final DbBuildHistoryLogService dbBuildHistoryLogService;
    private final ServerConfig serverConfig;
    private final WhitelistDirectoryService whitelistDirectoryService;

    public ProjectManageControl(OutGivingServer outGivingServer,
                                LogReadServer logReadServer,
                                MonitorService monitorService,
                                BuildInfoService buildService,
                                RepositoryService repositoryService,
                                ProjectInfoCacheService projectInfoCacheService,
                                DbBuildHistoryLogService dbBuildHistoryLogService,
                                ServerConfig serverConfig,
                                WhitelistDirectoryService whitelistDirectoryService) {
        this.outGivingServer = outGivingServer;
        this.logReadServer = logReadServer;
        this.monitorService = monitorService;
        this.buildService = buildService;
        this.repositoryService = repositoryService;
        this.projectInfoCacheService = projectInfoCacheService;
        this.dbBuildHistoryLogService = dbBuildHistoryLogService;
        this.serverConfig = serverConfig;
        this.whitelistDirectoryService = whitelistDirectoryService;
    }


    private void checkProjectPermission(String id, HttpServletRequest request, NodeModel node) {
        if (StrUtil.isNotEmpty(id)) {
            return;
        }
        String workspaceId = projectInfoCacheService.getCheckUserWorkspace(request);
        String fullId = ProjectInfoCacheModel.fullId(workspaceId, node.getId(), id);
        ProjectInfoCacheModel byKey = projectInfoCacheService.getByKey(fullId, request);
        Assert.notNull(byKey, "没有对应的数据或者没有此数据权限");
    }

    @RequestMapping(value = "getProjectData.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<JSONObject> getProjectData(@ValidatorItem String id, HttpServletRequest request) {
        NodeModel node = getNode();
        this.checkProjectPermission(id, request, node);
        JSONObject projectInfo = projectInfoCacheService.getItem(node, id);
        return JsonMessage.success("", projectInfo);
    }

    /**
     * get project access list
     * 获取项目的授权
     *
     * @return json
     * @author Hotstrip
     */
    @RequestMapping(value = "project-access-list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<List<String>> projectAccessList() {
        List<String> jsonArray = whitelistDirectoryService.getProjectDirectory(getNode());
        return JsonMessage.success("success", jsonArray);
    }

    /**
     * 保存项目
     *
     * @param id id
     * @return json
     */
    @RequestMapping(value = "saveProject", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<String> saveProject(String id, HttpServletRequest request) {
        NodeModel node = getNode();
        this.checkProjectPermission(id, request, node);
        //
        JsonMessage<String> jsonMessage = NodeForward.request(node, request, NodeUrl.Manage_SaveProject, "outGivingProject");
        if (jsonMessage.success()) {
            projectInfoCacheService.syncNode(node, id);
        }
        return jsonMessage;
    }


    /**
     * 释放分发
     *
     * @return json
     */
    @RequestMapping(value = "release-outgiving", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<String> releaseOutgiving(String id, HttpServletRequest request) {
        NodeModel node = getNode();
        this.checkProjectPermission(id, request, node);
        JsonMessage<String> jsonMessage = NodeForward.request(getNode(), request, NodeUrl.Manage_ReleaseOutGiving);
        if (jsonMessage.success()) {
            projectInfoCacheService.syncNode(node, id);
        }
        return jsonMessage;
    }

    /**
     * 获取正在运行的项目的端口和进程id
     *
     * @return json
     */
    @RequestMapping(value = "getProjectPort", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<JSONObject> getProjectPort(HttpServletRequest request) {
        return NodeForward.request(getNode(), request, NodeUrl.Manage_GetProjectPort);
    }


    /**
     * 查询所有项目
     *
     * @return json
     */
    @PostMapping(value = "get_project_info", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<PageResultDto<ProjectInfoCacheModel>> getProjectInfo(HttpServletRequest request) {
        PageResultDto<ProjectInfoCacheModel> modelPageResultDto = projectInfoCacheService.listPage(request);
        return JsonMessage.success("", modelPageResultDto);
    }

    /**
     * 删除项目
     *
     * @param id id
     * @return json
     */
    @PostMapping(value = "deleteProject", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public IJsonMessage<String> deleteProject(@ValidatorItem(value = ValidatorRule.NOT_BLANK) String id,

                                              HttpServletRequest request) {
        NodeModel nodeModel = getNode();
        this.checkProjectPermission(id, request, nodeModel);
        // 检查节点分发
        outGivingServer.checkNodeProject(nodeModel.getId(), id, request, "当前项目存在节点分发，不能直接删除");
        // 检查日志阅读
        logReadServer.checkNodeProject(nodeModel.getId(), id, request, "当前项目存在日志阅读，不能直接删除");
        // 项目监控
        List<MonitorModel> monitorModels = monitorService.listByWorkspace(request);
        if (monitorModels != null) {
            boolean match = monitorModels.stream().anyMatch(monitorModel -> monitorModel.checkNodeProject(nodeModel.getId(), id));
            Assert.state(!match, "当前项目存在监控项，不能直接删除");
        }
        // 构建
        boolean releaseMethod = buildService.checkReleaseMethod(nodeModel.getId() + StrUtil.COLON + id, request, BuildReleaseMethod.Project);
        Assert.state(!releaseMethod, "当前项目存在构建项，不能直接删除");

        JsonMessage<String> jsonMessage = NodeForward.request(nodeModel, request, NodeUrl.Manage_DeleteProject);
        if (jsonMessage.success()) {
            //
            projectInfoCacheService.syncExecuteNode(nodeModel);
        }
        return jsonMessage;
    }

    /**
     * 查看项目关联在线构建的数据
     *
     * @param projectData   项目数据
     * @param request       请求
     * @param toWorkspaceId 工作空间ID
     * @return list
     */
    private List<Tuple> checkBuild(ProjectInfoCacheModel projectData, String toWorkspaceId, HttpServletRequest request) {
        // 构建
        String dataId = projectData.getNodeId() + StrUtil.COLON + projectData.getProjectId();
        List<BuildInfoModel> buildInfoModels = buildService.listReleaseMethod(dataId, request, BuildReleaseMethod.Project);
        if (buildInfoModels != null) {
            return buildInfoModels.stream()
                .map(buildInfoModel -> {
                    // 判断共享仓库
                    RepositoryModel repositoryModel = repositoryService.getByKey(buildInfoModel.getRepositoryId());
                    Assert.notNull(repositoryModel, "仓库不存在");
                    if (StrUtil.equals(repositoryModel.getWorkspaceId(), toWorkspaceId)) {
                        // 迁移前后是同一个工作空间
                        return null;
                    }
                    // 非全局仓库判断仓库关联的构建
                    if (!repositoryModel.global()) {
                        BuildInfoModel buildInfoModel1 = new BuildInfoModel();
                        buildInfoModel1.setRepositoryId(buildInfoModel.getRepositoryId());
                        buildInfoModel1.setWorkspaceId(projectData.getWorkspaceId());
                        List<BuildInfoModel> infoModels = buildService.listByBean(buildInfoModel1);
                        if (CollUtil.size(infoModels) > 1) {
                            // 判断如果使用通过一个仓库
                            long count = infoModels.stream()
                                .filter(buildInfoModel2 -> {
                                    // 发布方式和数据id 不一样
                                    return !StrUtil.equals(buildInfoModel2.getReleaseMethodDataId(), dataId);
                                })
                                .count();
                            Assert.state(count <= 0, "当前【项目】关联的【在线构建】关联的【仓库(" + repositoryModel.getName() + ")】被其他 " + count + "个不同发布方式的【在线构建】绑定暂不支持迁移");
                        }
                    }
                    return new Tuple(buildInfoModel, repositoryModel);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    /**
     * 迁移项目关联在线构建的数据
     *
     * @param list          构建数据
     * @param toWorkspaceId 迁移到哪个工作空间
     * @return list
     */
    private String migrateBuild(List<Tuple> list, String toWorkspaceId, String toNodeId, ProjectInfoCacheModel projectData) {
        return list.stream()
            .map(tuple -> {
                BuildInfoModel infoModel = tuple.get(0);
                RepositoryModel repository = tuple.get(1);
                if (!repository.global()) {
                    // 非全局仓库才 修改仓库所属工作空间
                    String repositoryId = infoModel.getRepositoryId();
                    RepositoryModel repositoryModel = new RepositoryModel();
                    repositoryModel.setId(repositoryId);
                    repositoryModel.setWorkspaceId(toWorkspaceId);
                    repositoryService.updateById(repositoryModel);
                }
                //
                BuildInfoModel buildInfoModel = new BuildInfoModel();
                buildInfoModel.setId(infoModel.getId());
                buildInfoModel.setWorkspaceId(toWorkspaceId);
                // 修改发布的关联数据
                buildInfoModel.setReleaseMethodDataId(toNodeId + StrUtil.COLON + projectData.getProjectId());
                buildService.updateById(buildInfoModel);
                // 修改构建记录
                dbBuildHistoryLogService.update(
                    Entity.create().set("workspaceId", toWorkspaceId),
                    Entity.create().set("buildDataId", infoModel.getId())
                );
                if (!repository.global()) {
                    return StrUtil.format("自动迁移关联的构建：{} 和 仓库：{}", infoModel.getName(), repository.getName());
                }
                return StrUtil.format("自动迁移关联的构建：{}", infoModel.getName());
            }).
            collect(Collectors.joining(" | "));
    }

    /**
     * 迁移工作空间
     *
     * @param id id
     * @return json
     */
    @PostMapping(value = "migrate-workspace", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    @SystemPermission
    public IJsonMessage<String> migrateWorkspace(@ValidatorItem(value = ValidatorRule.NOT_BLANK) String id,
                                                 @ValidatorItem(value = ValidatorRule.NOT_BLANK) String toWorkspaceId,
                                                 @ValidatorItem(value = ValidatorRule.NOT_BLANK) String toNodeId,
                                                 HttpServletRequest request) {
        ProjectInfoCacheModel projectData = projectInfoCacheService.getByKey(id, request);
        Assert.notNull(projectData, "项目不存在");

        Assert.state(!StrUtil.equals(toWorkspaceId, projectData.getWorkspaceId()) || !StrUtil.equals(projectData.getNodeId(), toNodeId), "目标工作空间与当前工作空间一致并且目标节点与当前节点一致");
        projectInfoCacheService.checkUserWorkspace(toWorkspaceId);
        //
        NodeModel nowNode = nodeService.getByKey(projectData.getNodeId());
        Assert.notNull(nowNode, "当前对应的节点不存在");
        NodeModel toNodeModel = nodeService.getByKey(toNodeId);
        Assert.notNull(toNodeModel, "对应的节点不存在");
        Assert.state(StrUtil.equals(toWorkspaceId, toNodeModel.getWorkspaceId()), "要迁移到的目标工作空间和节点不一致");
        // 检查节点分发
        outGivingServer.checkNodeProject(projectData.getNodeId(), projectData.getProjectId(), request, "当前项目存在节点分发，不能直接迁移");
        // 检查日志阅读
        logReadServer.checkNodeProject(projectData.getNodeId(), projectData.getProjectId(), request, "当前项目存在日志阅读，不能直接迁移");
        // 项目监控
        List<MonitorModel> monitorModels = monitorService.listByWorkspace(request);
        if (monitorModels != null) {
            boolean match = monitorModels.stream().anyMatch(monitorModel -> monitorModel.checkNodeProject(projectData.getNodeId(), id));
            Assert.state(!match, "当前项目存在监控项，不能直接迁移");
        }
        // 检查构建
        List<Tuple> buildInfoModels = this.checkBuild(projectData, toWorkspaceId, request);
        JsonMessage<String> result;
        if (StrUtil.equals(nowNode.getMachineId(), toNodeModel.getMachineId())) {
            // 相同机器
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("newWorkspaceId", toWorkspaceId);
            jsonObject.put("newNodeId", toNodeId);
            jsonObject.put("id", projectData.getProjectId());
            JsonMessage<String> jsonMessage = NodeForward.request(nowNode, NodeUrl.Manage_ChangeWorkspaceId, jsonObject);
            if (!jsonMessage.success()) {
                return new JsonMessage<>(406, nowNode.getName() + "节点迁移项目失败" + jsonMessage.getMsg());
            }
            result = jsonMessage;
        } else {
            JSONObject item = projectInfoCacheService.getItem(nowNode, projectData.getProjectId());
            Assert.notNull(item, "项目数据丢失");
            item = projectInfoCacheService.convertToRequestData(item);
            item.put("nodeId", toNodeId);
            item.put("workspaceId", toWorkspaceId);
            item.put("previewData", true);
            // 发起预检查数据
            JsonMessage<String> jsonMessage = NodeForward.request(toNodeModel, NodeUrl.Manage_SaveProject, item);
            if (!jsonMessage.success()) {
                return new JsonMessage<>(406, toNodeModel.getName() + "节点与检查项目失败" + jsonMessage.getMsg());
            }
            item.remove("previewData");
            jsonMessage = NodeForward.request(toNodeModel, NodeUrl.Manage_SaveProject, item);
            if (!jsonMessage.success()) {
                return new JsonMessage<>(406, toNodeModel.getName() + "节点同步项目失败" + jsonMessage.getMsg());
            }
            // 删除之前节点项目
            JSONObject delData = new JSONObject();
            delData.put("id", projectData.getProjectId());
            // 非强制
            delData.put("thorough", "");
            JsonMessage<String> delJsonMeg = NodeForward.request(nowNode, NodeUrl.Manage_DeleteProject, delData);
            if (!delJsonMeg.success()) {
                return new JsonMessage<>(406, nowNode.getName() + "节点删除项目失败" + delJsonMeg.getMsg());
            }
            result = jsonMessage;
        }
        // 迁移构建
        String buildMsg = this.migrateBuild(buildInfoModels, toWorkspaceId, toNodeId, projectData);
        // 刷新缓存
        projectInfoCacheService.syncExecuteNode(nowNode);
        projectInfoCacheService.syncExecuteNode(toNodeModel);
        return new JsonMessage<>(200, "项目迁移成功：" + result.getMsg() + " | " + buildMsg);
    }

    /**
     * 操作项目
     * <p>
     * nodeId,id
     *
     * @return json
     */
    @RequestMapping(value = "operate", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public IJsonMessage<Object> operate(HttpServletRequest request) {
        NodeModel nodeModel = getNode();
        return NodeForward.request(nodeModel, request, NodeUrl.Manage_Operate);
    }


    /**
     * 下载导入模板
     */
    @GetMapping(value = "import-template", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public void importTemplate(HttpServletResponse response) throws IOException {
        String fileName = "项目导入模板.csv";
        this.setApplicationHeader(response, fileName);
        //
        CsvWriter writer = CsvUtil.getWriter(response.getWriter());
        writer.writeLine("id", "name", "groupName", "whitelistDirectory", "path", "logPath", "runMode",
            "mainClass",
            "jvm", "args",
            "javaExtDirsCp",
            "dslContent",
            "webHooks",
            "autoStart");
        writer.flush();
    }

    /**
     * 导出数据
     */
    @GetMapping(value = "export-data", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DOWNLOAD)
    public void exportData(HttpServletResponse response, HttpServletRequest request) throws IOException {
        String workspace = projectInfoCacheService.getCheckUserWorkspace(request);
        String fileName = "导出的项目数据 " + DateTime.now().toString(DatePattern.NORM_DATE_FORMAT) + ".csv";
        this.setApplicationHeader(response, fileName);
        //
        CsvWriteConfig csvWriteConfig = CsvWriteConfig.defaultConfig();
        csvWriteConfig.setAlwaysDelimitText(true);
        CsvWriter writer = CsvUtil.getWriter(response.getWriter(), csvWriteConfig);
        int pageInt = 0;
        writer.writeLine("id", "name", "groupName", "whitelistDirectory", "path", "logPath", "runMode",
            "mainClass",
            "jvm", "args", "javaExtDirsCp",
            "dslContent",
            "webHooks",
            "autoStart", "outGivingProject");
        while (true) {
            Map<String, String> paramMap = ServletUtil.getParamMap(request);
            // 下一页
            paramMap.put("page", String.valueOf(++pageInt));
            PageResultDto<ProjectInfoCacheModel> listPage = projectInfoCacheService.listPage(paramMap, false);
            if (listPage.isEmpty()) {
                break;
            }
            listPage.getResult()
                .stream()
                .map((Function<ProjectInfoCacheModel, List<Object>>) projectInfoCacheModel -> CollUtil.newArrayList(
                    projectInfoCacheModel.getProjectId(),
                    projectInfoCacheModel.getName(),
                    projectInfoCacheModel.getGroup(),
                    projectInfoCacheModel.getWhitelistDirectory(),
                    projectInfoCacheModel.getLib(),
                    projectInfoCacheModel.getLogPath(),
                    projectInfoCacheModel.getRunMode(),
                    projectInfoCacheModel.getMainClass(),
                    encodeCsv(projectInfoCacheModel.getJvm()),
                    encodeCsv(projectInfoCacheModel.getArgs()),
                    encodeCsv(projectInfoCacheModel.getJavaExtDirsCp()),
                    encodeCsv(projectInfoCacheModel.getDslContent()),
                    projectInfoCacheModel.getToken(),
                    projectInfoCacheModel.getAutoStart(),
                    projectInfoCacheModel.getOutGivingProject()
                ))
                .map(objects -> objects.stream().map(StrUtil::toStringOrNull).toArray(String[]::new))
                .forEach(writer::writeLine);
            if (ObjectUtil.equal(listPage.getPage(), listPage.getTotalPage())) {
                // 最后一页
                break;
            }
        }
        writer.flush();
    }


    private String encodeCsv(String data) {
        return StrUtil.replace(data, StrPool.LF, "\"\n\"");
    }

    private String decodeCsv(String data) {
        return StrUtil.replace(data, "\"\n\"", StrPool.LF);
    }

    /**
     * 导入数据
     *
     * @return json
     */
    @PostMapping(value = "import-data", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.UPLOAD)
    public IJsonMessage<String> importData(MultipartFile file, HttpServletRequest request) throws IOException {
        Assert.notNull(file, "没有上传文件");
        String workspaceId = projectInfoCacheService.getCheckUserWorkspace(request);
        NodeModel node = getNode();
        String originalFilename = file.getOriginalFilename();
        String extName = FileUtil.extName(originalFilename);
        Assert.state(StrUtil.endWithIgnoreCase(extName, "csv"), "不允许的文件格式");
        assert originalFilename != null;
        File csvFile = FileUtil.file(serverConfig.getUserTempPath(), originalFilename);
        int updateCount = 0, ignoreCount = 0;
        Charset fileCharset;
        try {
            file.transferTo(csvFile);
            //
            fileCharset = CharsetDetector.detect(csvFile);
            Reader bomReader = FileUtil.getReader(csvFile, fileCharset);
            CsvReadConfig csvReadConfig = CsvReadConfig.defaultConfig();
            csvReadConfig.setHeaderLineNo(0);
            CsvReader reader = CsvUtil.getReader(bomReader, csvReadConfig);
            CsvData csvData;
            try {
                csvData = reader.read();
            } catch (Exception e) {
                log.error("解析项目 csv 异常", e);
                return new JsonMessage<>(405, "解析文件异常," + e.getMessage());
            } finally {
                IoUtil.close(reader);
            }
            List<CsvRow> rows = csvData.getRows();
            Assert.notEmpty(rows, "没有任何数据");

            for (int i = 0; i < rows.size(); i++) {
                CsvRow csvRow = rows.get(i);
                JSONObject jsonObject = this.loadProjectData(csvRow, workspaceId, node);
                if (jsonObject == null) {
                    ignoreCount++;
                    continue;
                }
                try {
                    //
                    JsonMessage<String> jsonMessage = NodeForward.request(node, NodeUrl.Manage_SaveProject, jsonObject);
                    if (jsonMessage.success()) {
                        updateCount++;
                        continue;
                    }
                    throw new IllegalArgumentException(StrUtil.format("导入第 {} 条数据保存失败:{}", i + 2, jsonMessage.getMsg()));
                } catch (IllegalArgumentException | IllegalStateException e) {
                    throw Lombok.sneakyThrow(e);
                } catch (Exception e) {
                    log.error("导入保存项目异常", e);
                    throw new IllegalArgumentException(StrUtil.format("导入第 {} 条数据异常:{}", i + 2, e.getMessage()));
                }
            }
            projectInfoCacheService.syncExecuteNode(node);
        } finally {
            FileUtil.del(csvFile);
        }
        String fileCharsetStr = Optional.ofNullable(fileCharset).map(Charset::name).orElse(StrUtil.EMPTY);
        return JsonMessage.success("导入成功(编码格式：{}),更新 {} 条数据,因为节点分发/项目副本忽略 {} 条数据", fileCharsetStr, updateCount, ignoreCount);
    }

    private JSONObject loadProjectData(CsvRow csvRow, String workspaceId, NodeModel node) {
        String id = csvRow.getByName("id");
        String fullId = BaseNodeModel.fullId(workspaceId, node.getId(), id);

        ProjectInfoCacheModel projectInfoCacheModel1 = projectInfoCacheService.getByKey(fullId);
        if (projectInfoCacheModel1 != null) {
            // 节点分发项目不能在这里导入
            Boolean outGivingProject = projectInfoCacheModel1.getOutGivingProject();
            if (outGivingProject != null && outGivingProject) {
                return null;
            }
            if (StrUtil.isNotEmpty(projectInfoCacheModel1.getJavaCopyItemList())) {
                return null;
            }
        }
//        "id", "name", "groupName", "whitelistDirectory", "path", "logPath", "runMode",
//                "mainClass",
//                "jvm", "args", "javaExtDirsCp",
//                "dslContent",
//                "webHooks",
//                "autoStart", "outGivingProject"
        JSONObject data = new JSONObject();
        data.put("id", id);
        data.put("name", csvRow.getByName("name"));
        data.put("group", csvRow.getByName("groupName"));
        String runModeStr = csvRow.getByName("runMode");
        // 运行模式
        RunMode runMode1 = EnumUtil.fromString(RunMode.class, runModeStr, RunMode.ClassPath);
        data.put("runMode", runMode1.name());
        if (runMode1 == RunMode.ClassPath || runMode1 == RunMode.JavaExtDirsCp) {
            data.put("mainClass", csvRow.getByName("mainClass"));
        }
        if (runMode1 == RunMode.JavaExtDirsCp) {
            data.put("javaExtDirsCp", decodeCsv(csvRow.getByName("javaExtDirsCp")));
        }
        if (runMode1 == RunMode.Dsl) {
            data.put("dslContent", decodeCsv(csvRow.getByName("dslContent")));
        }
        data.put("whitelistDirectory", csvRow.getByName("whitelistDirectory"));
        data.put("logPath", csvRow.getByName("logPath"));
        data.put("lib", csvRow.getByName("path"));
        data.put("autoStart", csvRow.getByName("autoStart"));
        data.put("token", csvRow.getByName("webHooks"));
        data.put("jvm", decodeCsv(csvRow.getByName("jvm")));
        data.put("args", decodeCsv(csvRow.getByName("args")));
        return data;
    }
}
