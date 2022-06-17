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
package io.jpom.controller.node.system;

import cn.hutool.core.util.ReflectUtil;
import cn.jiangzeyin.common.JsonMessage;
import io.jpom.common.BaseServerController;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.data.AgentWhitelist;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.SystemPermission;
import io.jpom.service.system.WhitelistDirectoryService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 白名单目录
 *
 * @author jiangzeyin
 * @since 2019/2/28
 */
@RestController
@RequestMapping(value = "/node/system")
@Feature(cls = ClassFeature.NODE_CONFIG_WHITELIST)
@SystemPermission
public class WhitelistDirectoryController extends BaseServerController {

    private final WhitelistDirectoryService whitelistDirectoryService;

    public WhitelistDirectoryController(WhitelistDirectoryService whitelistDirectoryService) {
        this.whitelistDirectoryService = whitelistDirectoryService;
    }


    /**
     * get whiteList data
     * 白名单数据接口
     *
     * @return json
     * @author Hotstrip
     */
    @RequestMapping(value = "white-list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @SystemPermission
    public String whiteList() {
        AgentWhitelist agentWhitelist = whitelistDirectoryService.getData(getNode());
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
        return JsonMessage.getString(200, "ok", map);
    }


    /**
     * 保存接口
     *
     * @return json
     */
    @RequestMapping(value = "whitelistDirectory_submit", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @SystemPermission
    public String whitelistDirectorySubmit() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.WhitelistDirectory_Submit).toString();
    }
}
