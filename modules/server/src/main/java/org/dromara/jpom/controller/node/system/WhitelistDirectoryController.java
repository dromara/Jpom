/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.controller.node.system;

import cn.hutool.core.util.ReflectUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.forward.NodeUrl;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.func.assets.model.MachineNodeModel;
import org.dromara.jpom.model.data.AgentWhitelist;
import org.dromara.jpom.model.data.NodeModel;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.permission.SystemPermission;
import org.dromara.jpom.service.system.WhitelistDirectoryService;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 授权目录
 *
 * @author bwcx_jzy
 * @since 2019/2/28
 */
@RestController
@RequestMapping(value = "/node/system")
@Feature(cls = ClassFeature.NODE_CONFIG_WHITELIST)
public class WhitelistDirectoryController extends BaseServerController {

    private final WhitelistDirectoryService whitelistDirectoryService;

    public WhitelistDirectoryController(WhitelistDirectoryService whitelistDirectoryService) {
        this.whitelistDirectoryService = whitelistDirectoryService;
    }


    /**
     * get whiteList data
     * 授权数据接口
     *
     * @return json
     * @author Hotstrip
     */
    @RequestMapping(value = "white-list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<Map<String, String>> whiteList(String machineId) {
        NodeModel nodeModel = tryGetNode();
        AgentWhitelist agentWhitelist;
        if (nodeModel != null) {
            agentWhitelist = whitelistDirectoryService.getData(nodeModel);
        } else {
            MachineNodeModel machineNodeModel = machineNodeServer.getByKey(machineId);
            agentWhitelist = whitelistDirectoryService.getData(machineNodeModel);
        }
        Map<String, String> map = new HashMap<>(8);
        if (agentWhitelist != null) {
            /**
             * put key and value into map
             * 赋值给 map 对象返回
             */
            Field[] fields = ReflectUtil.getFields(AgentWhitelist.class, field -> Collection.class.isAssignableFrom(field.getType()) || String.class.isAssignableFrom(field.getType()));
            for (Field field : fields) {
                Object fieldValue = ReflectUtil.getFieldValue(agentWhitelist, field);
                if (fieldValue instanceof Collection) {
                    Collection<String> collection = (Collection<String>) fieldValue;
                    map.put(field.getName(), AgentWhitelist.convertToLine(collection));
                } else if (fieldValue instanceof String) {
                    map.put(field.getName(), (String) fieldValue);
                }
            }
        }
        return JsonMessage.success("", map);
    }


    /**
     * 保存接口
     *
     * @return json
     */
    @RequestMapping(value = "whitelistDirectory_submit", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @SystemPermission
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<String> whitelistDirectorySubmit(HttpServletRequest request, String machineId) {
        JsonMessage<String> objectJsonMessage = this.tryRequestNode(machineId, request, NodeUrl.WhitelistDirectory_Submit);
        Assert.notNull(objectJsonMessage, I18nMessageUtil.get("i18n.select_node.f8a6"));
        return objectJsonMessage;
    }
}
