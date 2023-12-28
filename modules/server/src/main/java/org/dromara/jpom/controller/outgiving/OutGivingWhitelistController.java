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
import cn.hutool.core.lang.RegexPool;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.func.files.service.StaticFileStorageService;
import org.dromara.jpom.model.data.AgentWhitelist;
import org.dromara.jpom.model.data.ServerWhitelist;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.permission.SystemPermission;
import org.dromara.jpom.service.system.SystemParametersServer;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 节点授权
 *
 * @author bwcx_jzy
 * @since 2019/4/22
 */
@RestController
@RequestMapping(value = "/outgiving")
@Feature(cls = ClassFeature.OUTGIVING_CONFIG_WHITELIST)
public class OutGivingWhitelistController extends BaseServerController {

    private final SystemParametersServer systemParametersServer;
    private final OutGivingWhitelistService outGivingWhitelistService;
    private final StaticFileStorageService staticFileStorageService;

    public OutGivingWhitelistController(SystemParametersServer systemParametersServer,
                                        OutGivingWhitelistService outGivingWhitelistService,
                                        StaticFileStorageService staticFileStorageService) {
        this.systemParametersServer = systemParametersServer;
        this.outGivingWhitelistService = outGivingWhitelistService;
        this.staticFileStorageService = staticFileStorageService;
    }


    /**
     * get whiteList data
     * 授权数据接口
     *
     * @return json
     * @author Hotstrip
     */
    @RequestMapping(value = "white-list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<Map<String, Object>> whiteList(HttpServletRequest request) {
        ServerWhitelist serverWhitelist = outGivingWhitelistService.getServerWhitelistData(request);
        Field[] fields = ReflectUtil.getFields(ServerWhitelist.class);
        Map<String, Object> map = new HashMap<>(8);
        for (Field field : fields) {
            Object value = ReflectUtil.getFieldValue(serverWhitelist, field);
            if (value instanceof Collection) {
                Collection<String> fieldValue = (Collection<String>) value;
                map.put(field.getName(), AgentWhitelist.convertToLine(fieldValue));
                map.put(field.getName() + "Array", fieldValue);
            }
        }
        return JsonMessage.success("ok", map);
    }


    /**
     * 保存节点授权
     *
     * @param outGiving 数据
     * @return json
     */
    @RequestMapping(value = "whitelistDirectory_submit", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @SystemPermission
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<String> whitelistDirectorySubmit(String outGiving,
                                                         String allowRemoteDownloadHost,
                                                         String staticDir,
                                                         HttpServletRequest request) {
        String workspaceId = nodeService.getCheckUserWorkspace(request);
        return this.whitelistDirectorySubmit(outGiving, staticDir, allowRemoteDownloadHost, workspaceId);
    }


    private IJsonMessage<String> whitelistDirectorySubmit(String outGiving,
                                                          String staticDir,
                                                          String allowRemoteDownloadHost,
                                                          String workspaceId) {
        List<String> list = AgentWhitelist.parseToList(outGiving, true, "授权目录不能为空");
        list = AgentWhitelist.covertToArray(list, "授权目录不能位于Jpom目录下");
        String error = AgentWhitelist.findStartsWith(list);
        Assert.isNull(error, "授权目录中不能存在包含关系：" + error);
        //
        List<String> staticDirList = AgentWhitelist.parseToList(staticDir, false, "静态目录授权不能为空");
        staticDirList = AgentWhitelist.covertToArray(staticDirList, 100, "静态目录授权不能位于Jpom目录下");
        error = AgentWhitelist.findStartsWith(staticDirList);
        Assert.isNull(error, "静态目录中不能存在包含关系：" + error);

        ServerWhitelist serverWhitelist = outGivingWhitelistService.getServerWhitelistData(workspaceId);
        serverWhitelist.setOutGiving(list);
        serverWhitelist.setStaticDir(staticDirList);
        //
        List<String> allowRemoteDownloadHostList = AgentWhitelist.parseToList(allowRemoteDownloadHost, "运行远程下载的 host 不能配置为空");
        //
        if (CollUtil.isNotEmpty(allowRemoteDownloadHostList)) {
            for (String s : allowRemoteDownloadHostList) {
                Assert.state(ReUtil.isMatch(RegexPool.URL_HTTP, s), "配置的远程地址不规范,请重新填写：" + s);
            }
        }
        serverWhitelist.setAllowRemoteDownloadHost(allowRemoteDownloadHostList == null ? null : CollUtil.newHashSet(allowRemoteDownloadHostList));
        //

        String id = ServerWhitelist.workspaceId(workspaceId);
        systemParametersServer.upsert(id, serverWhitelist, id);

        String resultData = AgentWhitelist.convertToLine(list);
        // 重新检查静态目录任务状态
        staticFileStorageService.startLoad();
        return JsonMessage.success("保存成功", resultData);
    }
}
