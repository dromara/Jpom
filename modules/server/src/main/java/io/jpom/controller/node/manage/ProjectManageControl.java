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
package io.jpom.controller.node.manage;

import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import io.jpom.common.BaseServerController;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.PageResultDto;
import io.jpom.model.data.MonitorModel;
import io.jpom.model.data.NodeModel;
import io.jpom.model.enums.BuildReleaseMethod;
import io.jpom.model.node.ProjectInfoCacheModel;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.permission.NodeDataPermission;
import io.jpom.service.dblog.BuildInfoService;
import io.jpom.service.monitor.MonitorService;
import io.jpom.service.node.ProjectInfoCacheService;
import io.jpom.service.outgiving.LogReadServer;
import io.jpom.service.outgiving.OutGivingServer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 项目管理
 *
 * @author Administrator
 */
@RestController
@RequestMapping(value = "/node/manage/")
@Feature(cls = ClassFeature.PROJECT)
@NodeDataPermission(cls = ProjectInfoCacheService.class)
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
    public String projectCopyList() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.Manage_ProjectCopyList).toString();
    }

    /**
     * 获取正在运行的项目的端口和进程id
     *
     * @return json
     */
    @RequestMapping(value = "getProjectPort", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String getProjectPort() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.Manage_GetProjectPort).toString();
    }

    /**
     * 获取正在运行的项目的端口和进程id
     *
     * @return json
     */
    @RequestMapping(value = "getProjectCopyPort", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String getProjectCopyPort() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.Manage_GetProjectCopyPort).toString();
    }


    /**
     * 查询所有项目
     *
     * @return json
     */
    @PostMapping(value = "get_project_info", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public String getProjectInfo() {
        PageResultDto<ProjectInfoCacheModel> modelPageResultDto = projectInfoCacheService.listPage(getRequest());
//		JSONArray jsonArray = projectInfoService.listAll(nodeModel, getRequest());
        return JsonMessage.getString(200, "", modelPageResultDto);
    }

    /**
     * 删除项目
     *
     * @param id id
     * @return json
     */
    @PostMapping(value = "deleteProject", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public String deleteProject(@ValidatorItem(value = ValidatorRule.NOT_BLANK) String id, String copyId) {
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
        JsonMessage<Object> request = NodeForward.request(nodeModel, servletRequest, NodeUrl.Manage_DeleteProject);
        if (request.getCode() == HttpStatus.OK.value()) {
            //
            projectInfoCacheService.syncNode(nodeModel);
        }
        return request.toString();
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
    public String restart() {
        NodeModel nodeModel = getNode();
        return NodeForward.request(nodeModel, getRequest(), NodeUrl.Manage_Restart).toString();
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
    public String start() {
        NodeModel nodeModel = getNode();
        return NodeForward.request(nodeModel, getRequest(), NodeUrl.Manage_Start).toString();
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
    public String stop() {
        NodeModel nodeModel = getNode();
        return NodeForward.request(nodeModel, getRequest(), NodeUrl.Manage_Stop).toString();
    }
}
