/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.controller.node.script;


import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.forward.NodeForward;
import org.dromara.jpom.common.forward.NodeUrl;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.model.PageResultDto;
import org.dromara.jpom.model.data.NodeModel;
import org.dromara.jpom.model.node.NodeScriptExecuteLogCacheModel;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.service.node.script.NodeScriptExecuteLogServer;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * @author bwcx_jzy
 * @since 2021/12/24
 */
@RestController
@RequestMapping(value = "/node/script_log")
@Feature(cls = ClassFeature.NODE_SCRIPT_LOG)
public class NodeScriptLogController extends BaseServerController {

    private final NodeScriptExecuteLogServer nodeScriptExecuteLogServer;

    public NodeScriptLogController(NodeScriptExecuteLogServer nodeScriptExecuteLogServer) {
        this.nodeScriptExecuteLogServer = nodeScriptExecuteLogServer;
    }

    /**
     * get script log list
     *
     * @return json
     */
    @RequestMapping(value = "list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<PageResultDto<NodeScriptExecuteLogCacheModel>> scriptList(HttpServletRequest request) {
        PageResultDto<NodeScriptExecuteLogCacheModel> pageResultDto = nodeScriptExecuteLogServer.listPage(request);
        return JsonMessage.success("", pageResultDto);
    }

    /**
     * 查日志
     *
     * @return json
     */
    @RequestMapping(value = "log", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<Object> log(HttpServletRequest request) {
        NodeModel node = getNode();
        return NodeForward.request(node, request, NodeUrl.SCRIPT_LOG);
    }

    /**
     * 删除日志
     *
     * @param id        模版ID
     * @param executeId 日志ID
     * @return json
     */
    @RequestMapping(value = "del", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public IJsonMessage<Object> del(@ValidatorItem String id, String executeId, HttpServletRequest request) {
        NodeModel node = getNode();
        NodeScriptExecuteLogCacheModel executeLogModel = nodeScriptExecuteLogServer.getByKey(executeId, request);
        Assert.notNull(executeLogModel, I18nMessageUtil.get("i18n.no_corresponding_execution_log.9545"));
        Assert.state(StrUtil.equals(id, executeLogModel.getScriptId()), I18nMessageUtil.get("i18n.data_associated_id_inconsistent.59f7"));
//        NodeScriptExecuteLogCacheModel nodeScriptExecuteLogCacheModel = new NodeScriptExecuteLogCacheModel();
//        nodeScriptExecuteLogCacheModel.setId(executeId);
//        nodeScriptExecuteLogCacheModel.setScriptId(id);
//        nodeScriptExecuteLogCacheModel.setNodeId(node.getId());
//        NodeScriptExecuteLogCacheModel executeLogModel = nodeScriptExecuteLogServer.queryByBean(nodeScriptExecuteLogCacheModel);

        JsonMessage<Object> jsonMessage = NodeForward.request(node, request, NodeUrl.SCRIPT_DEL_LOG);
        if (jsonMessage.success()) {
            nodeScriptExecuteLogServer.delByKey(executeId);
        }
        return jsonMessage;
    }
}
