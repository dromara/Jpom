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
import cn.hutool.core.io.BomReader;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.text.StrPool;
import cn.hutool.core.text.csv.*;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.JsonMessage;
import org.dromara.jpom.common.forward.NodeForward;
import org.dromara.jpom.common.forward.NodeUrl;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.common.validator.ValidatorRule;
import org.dromara.jpom.model.BaseNodeModel;
import org.dromara.jpom.model.RunMode;
import org.dromara.jpom.model.data.MonitorModel;
import org.dromara.jpom.model.data.NodeModel;
import org.dromara.jpom.model.enums.BuildReleaseMethod;
import org.dromara.jpom.model.node.ProjectInfoCacheModel;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.permission.NodeDataPermission;
import org.dromara.jpom.service.dblog.BuildInfoService;
import org.dromara.jpom.service.monitor.MonitorService;
import org.dromara.jpom.service.node.ProjectInfoCacheService;
import org.dromara.jpom.service.outgiving.LogReadServer;
import org.dromara.jpom.service.outgiving.OutGivingServer;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.dromara.jpom.model.PageResultDto;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 项目管理
 *
 * @author Administrator
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

    private final ProjectInfoCacheService projectInfoCacheService;

    public ProjectManageControl(OutGivingServer outGivingServer,
                                LogReadServer logReadServer,
                                MonitorService monitorService,
                                BuildInfoService buildService,
                                ProjectInfoCacheService projectInfoCacheService) {
        this.outGivingServer = outGivingServer;
        this.logReadServer = logReadServer;
        this.monitorService = monitorService;
        this.buildService = buildService;
        this.projectInfoCacheService = projectInfoCacheService;
    }


    /**
     * 展示项目页面
     */
    @RequestMapping(value = "project_copy_list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public JsonMessage<Object> projectCopyList() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.Manage_ProjectCopyList);
    }

    /**
     * 获取正在运行的项目的端口和进程id
     *
     * @return json
     */
    @RequestMapping(value = "getProjectPort", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonMessage<JSONObject> getProjectPort() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.Manage_GetProjectPort);
    }

    /**
     * 获取正在运行的项目的端口和进程id
     *
     * @return json
     */
    @RequestMapping(value = "getProjectCopyPort", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonMessage<JSONObject> getProjectCopyPort() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.Manage_GetProjectCopyPort);
    }


    /**
     * 查询所有项目
     *
     * @return json
     */
    @PostMapping(value = "get_project_info", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public JsonMessage<PageResultDto<ProjectInfoCacheModel>> getProjectInfo() {
        PageResultDto<ProjectInfoCacheModel> modelPageResultDto = projectInfoCacheService.listPage(getRequest());
//		JSONArray jsonArray = projectInfoService.listAll(nodeModel, getRequest());
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
    public JsonMessage<String> deleteProject(@ValidatorItem(value = ValidatorRule.NOT_BLANK) String id, String copyId) {
        NodeModel nodeModel = getNode();
        HttpServletRequest servletRequest = getRequest();
        if (StrUtil.isEmpty(copyId)) {
            // 检查节点分发
            outGivingServer.checkNodeProject(nodeModel.getId(), id, servletRequest);
            // 检查日志阅读
            logReadServer.checkNodeProject(nodeModel.getId(), id, servletRequest);
            //
            List<MonitorModel> monitorModels = monitorService.listByWorkspace(servletRequest);
            if (monitorModels != null) {
                boolean match = monitorModels.stream().anyMatch(monitorModel -> monitorModel.checkNodeProject(nodeModel.getId(), id));
//				if (monitorService.checkProject(nodeModel.getId(), id)) {
//					return JsonMessage.getString(405, );
//				}
                Assert.state(!match, "当前项目存在监控项，不能直接删除");
            }

            boolean releaseMethod = buildService.checkReleaseMethod(nodeModel.getId() + StrUtil.COLON + id, servletRequest, BuildReleaseMethod.Project);
            Assert.state(!releaseMethod, "当前项目存在构建项，不能直接删除");
        }
        JsonMessage<String> request = NodeForward.request(nodeModel, servletRequest, NodeUrl.Manage_DeleteProject);
        if (request.success()) {
            //
            projectInfoCacheService.syncExecuteNode(nodeModel);
        }
        return request;
    }

    /**
     * 重启项目
     * <p>
     * nodeId,id,copyId
     *
     * @return json
     */
    @RequestMapping(value = "restart", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public JsonMessage<Object> restart() {
        NodeModel nodeModel = getNode();
        return NodeForward.request(nodeModel, getRequest(), NodeUrl.Manage_Restart);
    }


    /**
     * 启动项目
     * <p>
     * nodeId,id,copyId
     *
     * @return json
     */
    @RequestMapping(value = "start", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public JsonMessage<Object> start() {
        NodeModel nodeModel = getNode();
        return NodeForward.request(nodeModel, getRequest(), NodeUrl.Manage_Start);
    }


    /**
     * 关闭项目项目
     * <p>
     * nodeId,id,copyId
     *
     * @return json
     */
    @RequestMapping(value = "stop", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public JsonMessage<Object> stop() {
        NodeModel nodeModel = getNode();
        return NodeForward.request(nodeModel, getRequest(), NodeUrl.Manage_Stop);
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
    public JsonMessage<String> importData(MultipartFile file, HttpServletRequest request) throws IOException {
        Assert.notNull(file, "没有上传文件");
        String workspaceId = projectInfoCacheService.getCheckUserWorkspace(request);
        NodeModel node = getNode();
        String originalFilename = file.getOriginalFilename();
        String extName = FileUtil.extName(originalFilename);
        Assert.state(StrUtil.endWithIgnoreCase(extName, "csv"), "不允许的文件格式");
        BomReader bomReader = IoUtil.getBomReader(file.getInputStream());
        CsvReadConfig csvReadConfig = CsvReadConfig.defaultConfig();
        csvReadConfig.setHeaderLineNo(0);
        CsvReader reader = CsvUtil.getReader(bomReader, csvReadConfig);
        CsvData csvData;
        try {
            csvData = reader.read();
        } catch (Exception e) {
            log.error("解析 csv 异常", e);
            return new JsonMessage<>(405, "解析文件异常," + e.getMessage());
        }
        List<CsvRow> rows = csvData.getRows();
        Assert.notEmpty(rows, "没有任何数据");
        int updateCount = 0, ignoreCount = 0;
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
        return JsonMessage.success("导入成功,更新 {} 条数据,因为节点分发/项目副本忽略 {} 条数据", updateCount, ignoreCount);
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
