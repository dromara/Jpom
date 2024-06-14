/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
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
import org.dromara.jpom.common.i18n.I18nMessageUtil;
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
    public IJsonMessage<String> save(@ValidatorItem String id, String type, HttpServletRequest request) {
        if ("add".equalsIgnoreCase(type)) {
            //boolean general = StringUtil.isGeneral(id, 2, 20);
            //Assert.state(general, "分发id 不能为空并且长度在2-20（英文字母 、数字和下划线）");
            String checkId = StrUtil.replace(id, StrUtil.DASHED, StrUtil.UNDERLINE);
            Validator.validateGeneral(checkId, 2, Const.ID_MAX_LEN, I18nMessageUtil.get("i18n.distribute_id_requirements.9c63"));
            return addOutGiving(id, request);
        } else {
            return updateGiving(id, request);
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
    public IJsonMessage<String> delete(String id, String thorough, HttpServletRequest request) {
        OutGivingModel outGivingModel = outGivingServer.getByKey(id, request);
        Assert.notNull(outGivingModel, I18nMessageUtil.get("i18n.no_corresponding_distribution_project.6dcd"));

        // 判断构建
        boolean releaseMethod = buildService.checkReleaseMethod(id, request, BuildReleaseMethod.Outgiving);
        Assert.state(!releaseMethod, I18nMessageUtil.get("i18n.distribution_with_build_items_message.45f5"));
        //
        Assert.state(outGivingModel.outGivingProject(), I18nMessageUtil.get("i18n.project_is_not_node_distribution_project_cannot_delete.2a5a"));

        List<OutGivingNodeProject> deleteNodeProject = outGivingModel.outGivingNodeProjectList();
        if (deleteNodeProject != null) {
            // 删除实际的项目
            for (OutGivingNodeProject outGivingNodeProject1 : deleteNodeProject) {
                NodeModel nodeModel = nodeService.getByKey(outGivingNodeProject1.getNodeId());
                JsonMessage<String> jsonMessage = this.deleteNodeProject(nodeModel, outGivingNodeProject1.getProjectId(), thorough);
                if (!jsonMessage.success()) {
                    return new JsonMessage<>(406, nodeModel.getName() + I18nMessageUtil.get("i18n.node_failed.20d5") + jsonMessage.getMsg());
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
        return JsonMessage.success(I18nMessageUtil.get("i18n.delete_success.0007"));
    }

    private IJsonMessage<String> addOutGiving(String id, HttpServletRequest request) {
        // 全局判断 id
        OutGivingModel outGivingModel = outGivingServer.getByKey(id);
        Assert.isNull(outGivingModel, I18nMessageUtil.get("i18n.distribute_id_already_exists.2168"));

        outGivingModel = new OutGivingModel();
        outGivingModel.setOutGivingProject(true);
        outGivingModel.setId(id);
        //
        List<Tuple> tuples = doData(outGivingModel, false, request);

        outGivingServer.insert(outGivingModel);
        IJsonMessage<String> error = saveNodeData(outGivingModel, tuples, false);
        return Optional.ofNullable(error).orElseGet(() -> JsonMessage.success(I18nMessageUtil.get("i18n.addition_succeeded.3fda")));
    }


    private IJsonMessage<String> updateGiving(String id, HttpServletRequest request) {
        OutGivingModel outGivingModel = outGivingServer.getByKey(id, request);
        Assert.notNull(outGivingModel, I18nMessageUtil.get("i18n.no_distribution_id_found.8df2"));
        List<Tuple> tuples = doData(outGivingModel, true, request);

        outGivingServer.updateById(outGivingModel);
        IJsonMessage<String> error = saveNodeData(outGivingModel, tuples, true);
        return Optional.ofNullable(error).orElseGet(() -> JsonMessage.success(I18nMessageUtil.get("i18n.modify_success.69be")));
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
                    return new JsonMessage<>(406, nodeModel.getName() + I18nMessageUtil.get("i18n.node_failed.20d5") + jsonMessage.getMsg());
                }
                success.add(tuple);
                // 同步项目信息
                projectInfoCacheService.syncNode(nodeModel, outGivingModel.getId());
            }
        } catch (Exception e) {
            log.error(I18nMessageUtil.get("i18n.save_distribution_project_failed.ceec"), e);
            if (!edit) {
                fail = true;
                outGivingServer.delByKey(outGivingModel.getId());
            }
            return new JsonMessage<>(500, I18nMessageUtil.get("i18n.save_node_data_failed.f314") + e.getMessage());
        } finally {
            if (fail) {
                try {
                    for (Tuple entry : success) {
                        deleteNodeProject(entry.get(0), outGivingModel.getId(), null);
                    }
                } catch (Exception e) {
                    log.error(I18nMessageUtil.get("i18n.restore_project_failed.7f7c"), e);
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
    private JSONObject getDefData(OutGivingModel outGivingModel, boolean edit, HttpServletRequest request) {
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
        ServerWhitelist configDeNewInstance = outGivingWhitelistService.getServerWhitelistData(request);
        List<String> whitelistServerOutGiving = configDeNewInstance.getOutGiving();
        Assert.state(AgentWhitelist.checkPath(whitelistServerOutGiving, whitelistDirectory), I18nMessageUtil.get("i18n.select_correct_project_path_or_no_auth_configured.366a"));

        defData.put("whitelistDirectory", whitelistDirectory);
        String logPath = getParameter("logPath");
        if (StrUtil.isNotEmpty(logPath)) {
            Assert.state(AgentWhitelist.checkPath(whitelistServerOutGiving, logPath), I18nMessageUtil.get("i18n.select_correct_log_path_or_no_auth_configured.9a9b"));
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
    private List<Tuple> doData(OutGivingModel outGivingModel, boolean edit, HttpServletRequest request) {
        outGivingModel.setName(getParameter("name"));
        outGivingModel.setGroup(getParameter("group"));
        Assert.hasText(outGivingModel.getName(), I18nMessageUtil.get("i18n.distribute_name_cannot_be_empty.0637"));
        //
        int intervalTime = getParameterInt("intervalTime", 10);
        outGivingModel.setIntervalTime(intervalTime);
        outGivingModel.setClearOld(Convert.toBool(getParameter("clearOld"), false));
        //
        String nodeIdsStr = getParameter("nodeIds");
        List<String> nodeIds = StrUtil.splitTrim(nodeIdsStr, StrUtil.COMMA);
        //List<NodeModel> nodeModelList = nodeService.listByWorkspace(request);
        Assert.notEmpty(nodeIds, I18nMessageUtil.get("i18n.no_node_info.6366"));

        //
        String afterOpt = getParameter("afterOpt");
        AfterOpt afterOpt1 = BaseEnum.getEnum(AfterOpt.class, Convert.toInt(afterOpt, 0));
        Assert.notNull(afterOpt1, I18nMessageUtil.get("i18n.post_distribution_action_required.8cc8"));
        outGivingModel.setAfterOpt(afterOpt1.getCode());
        JSONObject defData = getDefData(outGivingModel, edit, request);

        //
        List<OutGivingModel> outGivingModels = outGivingServer.list();
        List<OutGivingNodeProject> outGivingNodeProjects = new ArrayList<>();
        OutGivingNodeProject outGivingNodeProject;

        List<Tuple> tuples = new ArrayList<>();

        for (String nodeId : nodeIds) {
            NodeModel nodeModel = nodeService.getByKey(nodeId);
            Assert.notNull(nodeModel, I18nMessageUtil.get("i18n.node_not_exist.760e"));
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
                    Assert.state(!outGivingModel1.checkContains(nodeModel.getId(), outGivingModel.getId()), I18nMessageUtil.get("i18n.same_distribution_project_exists.ff41") + outGivingModel.getId());

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
            String disableScanDir = getParameter(StrUtil.format("{}_disableScanDir", nodeModel.getId()));
            allData.put("autoStart", Convert.toBool(autoStart, false));
            allData.put("disableScanDir", Convert.toBool(disableScanDir, false));
            allData.put("dslEnv", getParameter(StrUtil.format("{}_dslEnv", nodeModel.getId())));
            allData.put("nodeId", nodeModel.getId());
            JsonMessage<String> jsonMessage = this.sendData(nodeModel, allData, false);
            Assert.state(jsonMessage.success(), nodeModel.getName() + I18nMessageUtil.get("i18n.node_failed.20d5") + jsonMessage.getMsg());
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
                Validator.validateMatchRegex(RegexPool.URL_HTTP, s, I18nMessageUtil.get("i18n.invalid_webhooks_address.d836"));
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
        Assert.state(CollUtil.size(outGivingNodeProjects) >= 1, I18nMessageUtil.get("i18n.selected_node_required.d65a"));
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
                Assert.state(jsonMessage.success(), nodeModel.getName() + I18nMessageUtil.get("i18n.node_failed.20d5") + jsonMessage.getMsg());
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
