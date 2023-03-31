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
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.extra.servlet.ServletUtil;
import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.Const;
import org.dromara.jpom.common.JsonMessage;
import org.dromara.jpom.common.forward.NodeForward;
import org.dromara.jpom.common.forward.NodeUrl;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.model.AfterOpt;
import org.dromara.jpom.model.BaseEnum;
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
import org.dromara.jpom.model.PageResultDto;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 分发控制
 *
 * @author jiangzeyin
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
    public JsonMessage<PageResultDto<OutGivingModel>> dispatchList(HttpServletRequest request) {
        PageResultDto<OutGivingModel> pageResultDto = outGivingServer.listPage(request);
        return JsonMessage.success("success", pageResultDto);
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
    public JsonMessage<List<OutGivingModel>> dispatchListAll(HttpServletRequest request) {
        List<OutGivingModel> outGivingModels = outGivingServer.listByWorkspace(request);
        return JsonMessage.success("", outGivingModels);
    }


    @RequestMapping(value = "save", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public JsonMessage<String> save(String type, @ValidatorItem String id, HttpServletRequest request) throws IOException {
        if ("add".equalsIgnoreCase(type)) {
            //
            String checkId = StrUtil.replace(id, StrUtil.DASHED, StrUtil.UNDERLINE);
            Validator.validateGeneral(checkId, 2, Const.ID_MAX_LEN, "分发id 不能为空并且长度在2-20（英文字母 、数字和下划线）");
            //boolean general = StringUtil.isGeneral(id, 2, 20);
            //Assert.state(general, );
            return addOutGiving(id, request);
        } else {
            return updateGiving(id, request);
        }
    }

    private JsonMessage<String> addOutGiving(String id, HttpServletRequest request) {
        OutGivingModel outGivingModel = outGivingServer.getByKey(id);
        Assert.isNull(outGivingModel, "分发id已经存在啦,分发id需要全局唯一");
        //
        outGivingModel = new OutGivingModel();
        outGivingModel.setId(id);
        this.doData(outGivingModel, request);
        //
        outGivingServer.insert(outGivingModel);
        return JsonMessage.success("添加成功");
    }

    private JsonMessage<String> updateGiving(String id, HttpServletRequest request) {
        OutGivingModel outGivingModel = outGivingServer.getByKey(id, request);
        Assert.notNull(outGivingModel, "没有找到对应的分发id");
        doData(outGivingModel, request);

        outGivingServer.updateById(outGivingModel);
        return JsonMessage.success("修改成功");
    }

    private void doData(OutGivingModel outGivingModel, HttpServletRequest request) {
        outGivingModel.setName(getParameter("name"));
        outGivingModel.setGroup(getParameter("group"));
        Assert.hasText(outGivingModel.getName(), "分发名称不能为空");
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
                Assert.notNull(nodeModel, "不存在对应的节点");
                //
                boolean exists = projectInfoCacheService.exists(nodeModel.getWorkspaceId(), nodeModel.getId(), nodeIdProject);
                Assert.state(exists, "没有找到对应的项目id:" + nodeIdProject);
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
                        Assert.state(!checkContains, "已经存在相同的分发项目:" + outGivingNodeProject.getProjectId());
                    }
                }
            }).collect(Collectors.toList());

        Assert.state(CollUtil.size(outGivingNodeProjects) >= 1, "至少选择1个节点项目");

        outGivingModel.outGivingNodeProjectList(outGivingNodeProjects);
        //
        String afterOpt = getParameter("afterOpt");
        AfterOpt afterOpt1 = BaseEnum.getEnum(AfterOpt.class, Convert.toInt(afterOpt, 0));
        Assert.notNull(afterOpt1, "请选择分发后的操作");
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
                Validator.validateMatchRegex(RegexPool.URL_HTTP, s, "WebHooks 地址不合法");
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
    public JsonMessage<Object> releaseDel(String id, HttpServletRequest request) {
        // 判断构建
        boolean releaseMethod = buildService.checkReleaseMethod(id, request, BuildReleaseMethod.Outgiving);
        Assert.state(!releaseMethod, "当前分发存在构建项，不能删除");

        OutGivingModel outGivingServerItem = outGivingServer.getByKey(id, request);

        // 解除项目分发独立分发属性
        List<OutGivingNodeProject> outGivingNodeProjectList = outGivingServerItem.outGivingNodeProjectList();
        if (outGivingNodeProjectList != null) {
            outGivingNodeProjectList.forEach(outGivingNodeProject -> {
                NodeModel item = nodeService.getByKey(outGivingNodeProject.getNodeId());
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("id", outGivingNodeProject.getProjectId());
                JsonMessage<String> message = NodeForward.request(item, NodeUrl.Manage_ReleaseOutGiving, jsonObject);
                Assert.state(message.success(), "释放节点项目失败：" + message.getMsg());
            });
        }

        int byKey = outGivingServer.delByKey(id, request);
        if (byKey > 0) {
            // 删除日志
            Entity where = new Entity();
            where.set("outGivingId", id);
            dbOutGivingLogService.del(where);
        }
        return JsonMessage.success("操作成功");
    }

    /**
     * 解绑
     *
     * @param id 分发id
     * @return json
     */
    @GetMapping(value = "unbind.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public JsonMessage<Object> unbind(String id, HttpServletRequest request) {
        OutGivingModel outGivingServerItem = outGivingServer.getByKey(id, request);
        Assert.notNull(outGivingServerItem, "对应的分发不存在");
        // 判断构建
        boolean releaseMethod = buildService.checkReleaseMethod(id, request, BuildReleaseMethod.Outgiving);
        Assert.state(!releaseMethod, "当前分发存在构建项，不能解绑");

        int byKey = outGivingServer.delByKey(id, request);
        if (byKey > 0) {
            // 删除日志
            Entity where = new Entity();
            where.set("outGivingId", id);
            dbOutGivingLogService.del(where);
        }
        return JsonMessage.success("操作成功");
    }
}
