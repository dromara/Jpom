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
package org.dromara.jpom.controller.outgiving;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.lang.RegexPool;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.Const;
import org.dromara.jpom.common.forward.NodeForward;
import org.dromara.jpom.common.forward.NodeUrl;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.model.AfterOpt;
import org.dromara.jpom.model.BaseEnum;
import org.dromara.jpom.model.RunMode;
import org.dromara.jpom.model.data.AgentWhitelist;
import org.dromara.jpom.model.data.NodeModel;
import org.dromara.jpom.model.data.ServerWhitelist;
import org.dromara.jpom.model.enums.BuildReleaseMethod;
import org.dromara.jpom.model.outgiving.OutGivingModel;
import org.dromara.jpom.model.outgiving.OutGivingNodeProject;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.service.dblog.BuildInfoService;
import org.dromara.jpom.service.node.ProjectInfoCacheService;
import org.dromara.jpom.service.outgiving.DbOutGivingLogService;
import org.dromara.jpom.service.outgiving.OutGivingServer;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 节点分发编辑项目
 *
 * @author bwcx_jzy
 * @since 2019/4/22
 */
@RestController
@RequestMapping(value = "/outgiving")
@Feature(cls = ClassFeature.OUTGIVING)
@Slf4j
public class OutGivingProjectEditController extends BaseServerController {

    private final OutGivingWhitelistService outGivingWhitelistService;
    private final OutGivingServer outGivingServer;
    private final ProjectInfoCacheService projectInfoCacheService;
    private final BuildInfoService buildService;
    private final DbOutGivingLogService dbOutGivingLogService;

    public OutGivingProjectEditController(OutGivingWhitelistService outGivingWhitelistService,
                                          OutGivingServer outGivingServer,
                                          ProjectInfoCacheService projectInfoCacheService,
                                          BuildInfoService buildService,
                                          DbOutGivingLogService dbOutGivingLogService) {
        this.outGivingWhitelistService = outGivingWhitelistService;
        this.outGivingServer = outGivingServer;
        this.projectInfoCacheService = projectInfoCacheService;
        this.buildService = buildService;
        this.dbOutGivingLogService = dbOutGivingLogService;
    }

    /**
     * 保存节点分发项目
     *
     * @param id   id
     * @param type 类型
     * @return json
     */
    @RequestMapping(value = "save_project", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<String> save(@ValidatorItem String id, String type) {
        if ("add".equalsIgnoreCase(type)) {
            //boolean general = StringUtil.isGeneral(id, 2, 20);
            //Assert.state(general, "分发id 不能为空并且长度在2-20（英文字母 、数字和下划线）");
            String checkId = StrUtil.replace(id, StrUtil.DASHED, StrUtil.UNDERLINE);
            Validator.validateGeneral(checkId, 2, Const.ID_MAX_LEN, "分发id 不能为空并且长度在2-20（英文字母 、数字和下划线）");
            return addOutGiving(id);
        } else {
            return updateGiving(id);
        }
    }

    /**
     * 删除分发项目
     *
     * @param id 项目id
     * @return json
     */
    @RequestMapping(value = "delete_project", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public IJsonMessage<String> delete(String id, String thorough) {
        HttpServletRequest request = getRequest();
        OutGivingModel outGivingModel = outGivingServer.getByKey(id, request);
        Assert.notNull(outGivingModel, "没有对应的分发项目");

        // 判断构建
        boolean releaseMethod = buildService.checkReleaseMethod(id, request, BuildReleaseMethod.Outgiving);
        Assert.state(!releaseMethod, "当前分发存在构建项，不能删除");
        //
        Assert.state(outGivingModel.outGivingProject(), "该项目不是节点分发项目,不能在此次删除");

        List<OutGivingNodeProject> deleteNodeProject = outGivingModel.outGivingNodeProjectList();
        if (deleteNodeProject != null) {
            // 删除实际的项目
            for (OutGivingNodeProject outGivingNodeProject1 : deleteNodeProject) {
                NodeModel nodeModel = nodeService.getByKey(outGivingNodeProject1.getNodeId());
                JsonMessage<String> jsonMessage = this.deleteNodeProject(nodeModel, outGivingNodeProject1.getProjectId(), thorough);
                if (!jsonMessage.success()) {
                    return new JsonMessage<>(406, nodeModel.getName() + "节点失败：" + jsonMessage.getMsg());
                }
            }
        }

        int byKey = outGivingServer.delByKey(id, request);
        // 删除日志
        if (byKey > 0) {
            Entity where = new Entity();
            where.set("outGivingId", id);
            dbOutGivingLogService.del(where);
        }
        return JsonMessage.success("删除成功");
    }

    private IJsonMessage<String> addOutGiving(String id) {
        OutGivingModel outGivingModel = outGivingServer.getByKey(id);
        Assert.isNull(outGivingModel, "分发id已经存在啦");

        outGivingModel = new OutGivingModel();
        outGivingModel.setOutGivingProject(true);
        outGivingModel.setId(id);
        //
        List<Tuple> tuples = doData(outGivingModel, false);

        outGivingServer.insert(outGivingModel);
        IJsonMessage<String> error = saveNodeData(outGivingModel, tuples, false);
        return Optional.ofNullable(error).orElseGet(() -> JsonMessage.success("添加成功"));
    }


    private IJsonMessage<String> updateGiving(String id) {
        OutGivingModel outGivingModel = outGivingServer.getByKey(id, getRequest());
        Assert.notNull(outGivingModel, "没有找到对应的分发id");
        List<Tuple> tuples = doData(outGivingModel, true);

        outGivingServer.updateById(outGivingModel);
        IJsonMessage<String> error = saveNodeData(outGivingModel, tuples, true);
        return Optional.ofNullable(error).orElseGet(() -> JsonMessage.success("修改成功"));
    }

    /**
     * 保存节点项目数据
     *
     * @param outGivingModel 节点分发项目
     * @param edit           是否为编辑模式
     * @return 错误信息
     */
    private JsonMessage<String> saveNodeData(OutGivingModel outGivingModel, List<Tuple> tuples, boolean edit) {

//		if () {
//			if (!edit) {
//				outGivingServer.delByKey(outGivingModel.getId());
//			}
//			return JsonMessage.getString(405, "数据异常,请重新操作");
//		}
        List<Tuple> success = new ArrayList<>();
        boolean fail = false;
        try {
            for (Tuple tuple : tuples) {
                NodeModel nodeModel = tuple.get(0);
                JSONObject data = tuple.get(1);
                //
                JsonMessage<String> jsonMessage = this.sendData(nodeModel, data, true);
                if (!jsonMessage.success()) {
                    if (!edit) {
                        fail = true;
                        outGivingServer.delByKey(outGivingModel.getId());
                    }
                    return new JsonMessage<>(406, nodeModel.getName() + "节点失败：" + jsonMessage.getMsg());
                }
                success.add(tuple);
                // 同步项目信息
                projectInfoCacheService.syncNode(nodeModel, outGivingModel.getId());
            }
        } catch (Exception e) {
            log.error("保存分发项目失败", e);
            if (!edit) {
                fail = true;
                outGivingServer.delByKey(outGivingModel.getId());
            }
            return new JsonMessage<>(500, "保存节点数据失败:" + e.getMessage());
        } finally {
            if (fail) {
                try {
                    for (Tuple entry : success) {
                        deleteNodeProject(entry.get(0), outGivingModel.getId(), null);
                    }
                } catch (Exception e) {
                    log.error("还原项目失败", e);
                }
            }
        }
        return null;
    }

    /**
     * 删除项目
     *
     * @param nodeModel 节点
     * @param project   判断id
     * @param thorough  是否彻底删除
     * @return json
     */
    private JsonMessage<String> deleteNodeProject(NodeModel nodeModel, String project, String thorough) {
        JSONObject data = new JSONObject();
        data.put("id", project);
        data.put("thorough", thorough);
        JsonMessage<String> request = NodeForward.request(nodeModel, NodeUrl.Manage_DeleteProject, data);
        if (request.success()) {
            // 同步项目信息
            projectInfoCacheService.syncNode(nodeModel, project);
        }
        return request;
//        // 发起预检查数据
//        String url = nodeModel.getRealUrl(NodeUrl.Manage_DeleteProject);
//        HttpRequest request = HttpUtil.createPost(url);
//        // 授权信息
//        NodeForward.addUser(request, nodeModel, userModel);

//        request.form(data);
//        //
//        String body = request.execute()
//                .body();
//        return NodeForward.toJsonMessage(body);
    }

    /**
     * 创建项目管理的默认数据
     *
     * @param outGivingModel 分发实体
     * @param edit           是否为编辑模式
     * @return String为有异常
     */
    private JSONObject getDefData(OutGivingModel outGivingModel, boolean edit) {
        JSONObject defData = new JSONObject();
        defData.put("id", outGivingModel.getId());
        defData.put("name", outGivingModel.getName());
        defData.put("group", outGivingModel.getGroup());
        //
        // 运行模式
        String runMode = getParameter("runMode");
        RunMode runMode1 = EnumUtil.fromString(RunMode.class, runMode, RunMode.ClassPath);
        defData.put("runMode", runMode1.name());
        if (runMode1 == RunMode.ClassPath || runMode1 == RunMode.JavaExtDirsCp) {
            String mainClass = getParameter("mainClass");
            defData.put("mainClass", mainClass);
        }
        if (runMode1 == RunMode.JavaExtDirsCp) {
            defData.put("javaExtDirsCp", getParameter("javaExtDirsCp"));
        }
        if (runMode1 == RunMode.Dsl) {
            defData.put("dslContent", getParameter("dslContent"));
        }
        String whitelistDirectory = getParameter("whitelistDirectory");
        ServerWhitelist configDeNewInstance = outGivingWhitelistService.getServerWhitelistData(getRequest());
        List<String> whitelistServerOutGiving = configDeNewInstance.outGiving();
        Assert.state(AgentWhitelist.checkPath(whitelistServerOutGiving, whitelistDirectory), "请选择正确的项目路径,或者还没有配置白名单");

        defData.put("whitelistDirectory", whitelistDirectory);
        String logPath = getParameter("logPath");
        if (StrUtil.isNotEmpty(logPath)) {
            Assert.state(AgentWhitelist.checkPath(whitelistServerOutGiving, logPath), "请选择正确的日志路径,或者还没有配置白名单");
            defData.put("logPath", logPath);
        }
        String lib = getParameter("lib");
        defData.put("lib", lib);
        if (edit) {
            // 编辑模式
            defData.put("edit", "on");
        }
        defData.put("previewData", true);
        return defData;
    }

    /**
     * 处理页面数据
     *
     * @param outGivingModel 分发实体
     * @param edit           是否为编辑模式
     */
    private List<Tuple> doData(OutGivingModel outGivingModel, boolean edit) {
        outGivingModel.setName(getParameter("name"));
        outGivingModel.setGroup(getParameter("group"));
        Assert.hasText(outGivingModel.getName(), "分发名称不能为空");
        //
        int intervalTime = getParameterInt("intervalTime", 10);
        outGivingModel.setIntervalTime(intervalTime);
        outGivingModel.setClearOld(Convert.toBool(getParameter("clearOld"), false));
        //
        String nodeIdsStr = getParameter("nodeIds");
        List<String> nodeIds = StrUtil.splitTrim(nodeIdsStr, StrUtil.COMMA);
        //List<NodeModel> nodeModelList = nodeService.listByWorkspace(getRequest());
        Assert.notEmpty(nodeIds, "没有任何节点信息");

        //
        String afterOpt = getParameter("afterOpt");
        AfterOpt afterOpt1 = BaseEnum.getEnum(AfterOpt.class, Convert.toInt(afterOpt, 0));
        Assert.notNull(afterOpt1, "请选择分发后的操作");
        outGivingModel.setAfterOpt(afterOpt1.getCode());
        JSONObject defData = getDefData(outGivingModel, edit);

        //
        List<OutGivingModel> outGivingModels = outGivingServer.list();
        List<OutGivingNodeProject> outGivingNodeProjects = new ArrayList<>();
        OutGivingNodeProject outGivingNodeProject;

        List<Tuple> tuples = new ArrayList<>();

        for (String nodeId : nodeIds) {
            NodeModel nodeModel = nodeService.getByKey(nodeId);
            Assert.notNull(nodeModel, "对应的节点不存在");
            //String add = getParameter("add_" + nodeModel.getId());
//			if (!nodeModel.getId().equals(add)) {
//				iterator.remove();
//				continue;
//			}
            // 判断项目是否已经被使用过啦
            if (outGivingModels != null) {
                for (OutGivingModel outGivingModel1 : outGivingModels) {
                    if (outGivingModel1.getId().equalsIgnoreCase(outGivingModel.getId())) {
                        continue;
                    }
                    Assert.state(!outGivingModel1.checkContains(nodeModel.getId(), outGivingModel.getId()), "已经存在相同的分发项目:" + outGivingModel.getId());

                }
            }
            outGivingNodeProject = outGivingModel.getNodeProject(nodeModel.getId(), outGivingModel.getId());
            if (outGivingNodeProject == null) {
                outGivingNodeProject = new OutGivingNodeProject();
            }
            outGivingNodeProject.setNodeId(nodeModel.getId());
            // 分发id为项目id
            outGivingNodeProject.setProjectId(outGivingModel.getId());
            outGivingNodeProjects.add(outGivingNodeProject);
            // 检查数据
            JSONObject allData = defData.clone();
            String token = getParameter(StrUtil.format("{}_token", nodeModel.getId()));
            allData.put("token", token);
            String jvm = getParameter(StrUtil.format("{}_jvm", nodeModel.getId()));
            allData.put("jvm", jvm);
            String args = getParameter(StrUtil.format("{}_args", nodeModel.getId()));
            allData.put("args", args);
            String autoStart = getParameter(StrUtil.format("{}_autoStart", nodeModel.getId()));
            allData.put("autoStart", Convert.toBool(autoStart, false));
            allData.put("dslEnv", getParameter(StrUtil.format("{}_dslEnv", nodeModel.getId())));

            JsonMessage<String> jsonMessage = this.sendData(nodeModel, allData, false);
            Assert.state(jsonMessage.success(), nodeModel.getName() + "节点失败：" + jsonMessage.getMsg());
            tuples.add(new Tuple(nodeModel, allData));
        }
        // 删除已经删除的项目
        deleteProject(outGivingModel, outGivingNodeProjects);

        outGivingModel.outGivingNodeProjectList(outGivingNodeProjects);
        //
        String secondaryDirectory = getParameter("secondaryDirectory");
        outGivingModel.setSecondaryDirectory(secondaryDirectory);
        outGivingModel.setUploadCloseFirst(Convert.toBool(getParameter("uploadCloseFirst"), false));
        //
        String webhook = getParameter("webhook");
        webhook = Opt.ofBlankAble(webhook)
            .map(s -> {
                Validator.validateMatchRegex(RegexPool.URL_HTTP, s, "WebHooks 地址不合法");
                return s;
            })
            .orElse(StrUtil.EMPTY);
        outGivingModel.setWebhook(webhook);
        return tuples;
    }

    /**
     * 删除已经删除过的项目
     *
     * @param outGivingModel        分发项目
     * @param outGivingNodeProjects 新的节点项目
     */
    private void deleteProject(OutGivingModel outGivingModel, List<OutGivingNodeProject> outGivingNodeProjects) {
        Assert.state(CollUtil.size(outGivingNodeProjects) >= 1, "至少选择一个节点及以上");
        // 删除
        List<OutGivingNodeProject> deleteNodeProject = outGivingModel.getDelete(outGivingNodeProjects);
        if (deleteNodeProject != null) {
            JsonMessage<String> jsonMessage;
            // 删除实际的项目
            for (OutGivingNodeProject outGivingNodeProject1 : deleteNodeProject) {
                NodeModel nodeModel = nodeService.getByKey(outGivingNodeProject1.getNodeId());
                //outGivingNodeProject1.getNodeData(true);
                // 调用彻底删除
                jsonMessage = this.deleteNodeProject(nodeModel, outGivingNodeProject1.getProjectId(), "thorough");
                Assert.state(jsonMessage.success(), nodeModel.getName() + "节点失败：" + jsonMessage.getMsg());
            }
        }
    }

    private JsonMessage<String> sendData(NodeModel nodeModel, JSONObject data, boolean save) {
        if (save) {
            data.remove("previewData");
        }
        data.put("outGivingProject", true);
        // 发起预检查数据
        return NodeForward.request(nodeModel, NodeUrl.Manage_SaveProject, data);
    }
}
