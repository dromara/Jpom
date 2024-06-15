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
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.extra.servlet.ServletUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.Const;
import org.dromara.jpom.common.forward.NodeForward;
import org.dromara.jpom.common.forward.NodeUrl;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.model.AfterOpt;
import org.dromara.jpom.model.BaseEnum;
import org.dromara.jpom.model.PageResultDto;
import org.dromara.jpom.model.data.NodeModel;
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
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 分发控制
 *
 * @author bwcx_jzy
 * @since 2019/4/20
 */
@RestController
@RequestMapping(value = "/outgiving")
@Feature(cls = ClassFeature.OUTGIVING)
public class OutGivingController extends BaseServerController {

    private final OutGivingServer outGivingServer;
    private final BuildInfoService buildService;
    private final DbOutGivingLogService dbOutGivingLogService;
    private final ProjectInfoCacheService projectInfoCacheService;

    public OutGivingController(OutGivingServer outGivingServer,
                               BuildInfoService buildService,
                               DbOutGivingLogService dbOutGivingLogService,
                               ProjectInfoCacheService projectInfoCacheService) {
        this.outGivingServer = outGivingServer;
        this.buildService = buildService;
        this.dbOutGivingLogService = dbOutGivingLogService;
        this.projectInfoCacheService = projectInfoCacheService;
    }

    /**
     * load dispatch list
     * 加载分发列表
     *
     * @return json
     * @author Hotstrip
     */
    @PostMapping(value = "dispatch-list", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<PageResultDto<OutGivingModel>> dispatchList(HttpServletRequest request) {
        PageResultDto<OutGivingModel> pageResultDto = outGivingServer.listPage(request);
        return JsonMessage.success("", pageResultDto);
    }

    /**
     * load dispatch list
     * 加载分发列表
     *
     * @return json
     * @author Hotstrip
     */
    @GetMapping(value = "dispatch-list-all", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<List<OutGivingModel>> dispatchListAll(HttpServletRequest request) {
        List<OutGivingModel> outGivingModels = outGivingServer.listByWorkspace(request);
        return JsonMessage.success("", outGivingModels);
    }


    @RequestMapping(value = "save", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<String> save(String type, @ValidatorItem String id, HttpServletRequest request) throws IOException {
        if ("add".equalsIgnoreCase(type)) {
            //
            String checkId = StrUtil.replace(id, StrUtil.DASHED, StrUtil.UNDERLINE);
            Validator.validateGeneral(checkId, 2, Const.ID_MAX_LEN, I18nMessageUtil.get("i18n.distribute_id_requirements.9c63"));
            //boolean general = StringUtil.isGeneral(id, 2, 20);
            //Assert.state(general, );
            return addOutGiving(id, request);
        } else {
            return updateGiving(id, request);
        }
    }

    private IJsonMessage<String> addOutGiving(String id, HttpServletRequest request) {
        OutGivingModel outGivingModel = outGivingServer.getByKey(id);
        Assert.isNull(outGivingModel, I18nMessageUtil.get("i18n.distribute_id_already_exists_globally.6478"));
        //
        outGivingModel = new OutGivingModel();
        outGivingModel.setId(id);
        this.doData(outGivingModel, request);
        //
        outGivingServer.insert(outGivingModel);
        return JsonMessage.success(I18nMessageUtil.get("i18n.addition_succeeded.3fda"));
    }

    private IJsonMessage<String> updateGiving(String id, HttpServletRequest request) {
        OutGivingModel outGivingModel = outGivingServer.getByKey(id, request);
        Assert.notNull(outGivingModel, I18nMessageUtil.get("i18n.no_distribution_id_found.8df2"));
        doData(outGivingModel, request);

        outGivingServer.updateById(outGivingModel);
        return JsonMessage.success(I18nMessageUtil.get("i18n.modify_success.69be"));
    }

    private void doData(OutGivingModel outGivingModel, HttpServletRequest request) {
        outGivingModel.setName(getParameter("name"));
        outGivingModel.setGroup(getParameter("group"));
        Assert.hasText(outGivingModel.getName(), I18nMessageUtil.get("i18n.distribute_name_cannot_be_empty.0637"));
        List<OutGivingModel> outGivingModels = outGivingServer.list();
        //
        Map<String, String> paramMap = ServletUtil.getParamMap(request);
        List<OutGivingNodeProject> outGivingNodeProjects = paramMap.entrySet()
            .stream()
            .filter(stringStringEntry -> StrUtil.startWith(stringStringEntry.getKey(), "node_"))
            .map(stringStringEntry -> {
                int lastIndexOf = StrUtil.lastIndexOfIgnoreCase(stringStringEntry.getKey(), StrUtil.UNDERLINE);
                int indexOf = StrUtil.indexOfIgnoreCase(stringStringEntry.getKey(), StrUtil.UNDERLINE) + 1;
                String nodeId = StrUtil.sub(stringStringEntry.getKey(), indexOf, lastIndexOf);
                //
                String nodeIdProject = stringStringEntry.getValue();
                NodeModel nodeModel = nodeService.getByKey(nodeId);
                Assert.notNull(nodeModel, I18nMessageUtil.get("i18n.node_not_exist.0027"));
                //
                boolean exists = projectInfoCacheService.exists(nodeModel.getWorkspaceId(), nodeModel.getId(), nodeIdProject);
                Assert.state(exists, I18nMessageUtil.get("i18n.no_project_id_found.0f21") + nodeIdProject);
                //
                OutGivingNodeProject outGivingNodeProject = outGivingModel.getNodeProject(nodeModel.getId(), nodeIdProject);
                if (outGivingNodeProject == null) {
                    outGivingNodeProject = new OutGivingNodeProject();
                }
                outGivingNodeProject.setNodeId(nodeModel.getId());
                outGivingNodeProject.setProjectId(nodeIdProject);
                return outGivingNodeProject;
            })
            .peek(outGivingNodeProject -> {
                // 判断项目是否已经被使用过啦
                if (outGivingModels != null) {
                    for (OutGivingModel outGivingModel1 : outGivingModels) {
                        if (outGivingModel1.getId().equalsIgnoreCase(outGivingModel.getId())) {
                            continue;
                        }
                        boolean checkContains = outGivingModel1.checkContains(outGivingNodeProject.getNodeId(), outGivingNodeProject.getProjectId());
                        Assert.state(!checkContains, I18nMessageUtil.get("i18n.same_distribution_project_exists.ff41") + outGivingNodeProject.getProjectId());
                    }
                }
            }).collect(Collectors.toList());

        Assert.state(CollUtil.size(outGivingNodeProjects) >= 1, I18nMessageUtil.get("i18n.select_at_least_one_node_project.637c"));

        outGivingModel.outGivingNodeProjectList(outGivingNodeProjects);
        //
        String afterOpt = getParameter("afterOpt");
        AfterOpt afterOpt1 = BaseEnum.getEnum(AfterOpt.class, Convert.toInt(afterOpt, 0));
        Assert.notNull(afterOpt1, I18nMessageUtil.get("i18n.post_distribution_action_required.8cc8"));
        outGivingModel.setAfterOpt(afterOpt1.getCode());
        //
        int intervalTime = getParameterInt("intervalTime", 10);
        outGivingModel.setIntervalTime(intervalTime);
        //
        outGivingModel.setClearOld(Convert.toBool(getParameter("clearOld"), false));
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
    }

    /**
     * 删除分发信息
     *
     * @param id 分发id
     * @return json
     */
    @RequestMapping(value = "release_del.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public IJsonMessage<Object> releaseDel(String id, HttpServletRequest request) {
        // 判断构建
        boolean releaseMethod = buildService.checkReleaseMethod(id, request, BuildReleaseMethod.Outgiving);
        Assert.state(!releaseMethod, I18nMessageUtil.get("i18n.distribution_with_build_items_message.45f5"));

        OutGivingModel outGivingServerItem = outGivingServer.getByKey(id, request);

        // 解除项目分发独立分发属性
        List<OutGivingNodeProject> outGivingNodeProjectList = outGivingServerItem.outGivingNodeProjectList();
        if (outGivingNodeProjectList != null) {
            outGivingNodeProjectList.forEach(outGivingNodeProject -> {
                NodeModel item = nodeService.getByKey(outGivingNodeProject.getNodeId());
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("id", outGivingNodeProject.getProjectId());
                JsonMessage<String> message = NodeForward.request(item, NodeUrl.Manage_ReleaseOutGiving, jsonObject);
                Assert.state(message.success(), I18nMessageUtil.get("i18n.release_node_project_failed.764e") + message.getMsg());
            });
        }

        int byKey = outGivingServer.delByKey(id, request);
        if (byKey > 0) {
            // 删除日志
            Entity where = new Entity();
            where.set("outGivingId", id);
            dbOutGivingLogService.del(where);
        }
        return JsonMessage.success(I18nMessageUtil.get("i18n.operation_succeeded.3313"));
    }

    /**
     * 解绑
     *
     * @param id 分发id
     * @return json
     */
    @GetMapping(value = "unbind.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public IJsonMessage<Object> unbind(String id, HttpServletRequest request) {
        OutGivingModel outGivingServerItem = outGivingServer.getByKey(id, request);
        Assert.notNull(outGivingServerItem, I18nMessageUtil.get("i18n.distribution_not_exist.cf8a"));
        // 判断构建
        boolean releaseMethod = buildService.checkReleaseMethod(id, request, BuildReleaseMethod.Outgiving);
        Assert.state(!releaseMethod, I18nMessageUtil.get("i18n.current_distribution_has_build_items_cannot_unbind.a8e9"));

        int byKey = outGivingServer.delByKey(id, request);
        if (byKey > 0) {
            // 删除日志
            Entity where = new Entity();
            where.set("outGivingId", id);
            dbOutGivingLogService.del(where);
        }
        return JsonMessage.success(I18nMessageUtil.get("i18n.operation_succeeded.3313"));
    }
}
