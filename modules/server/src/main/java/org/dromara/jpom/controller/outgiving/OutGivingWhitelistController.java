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
import cn.hutool.core.lang.RegexPool;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.i18n.I18nThreadUtil;
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
@Slf4j
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
        return JsonMessage.success("", map);
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
        List<String> list = AgentWhitelist.parseToList(outGiving, true, I18nMessageUtil.get("i18n.auth_directory_cannot_be_empty.21ba"));
        list = AgentWhitelist.covertToArray(list, I18nMessageUtil.get("i18n.auth_directory_cannot_be_under_jpom.bb67"));
        String error = AgentWhitelist.findStartsWith(list);
        Assert.isNull(error, I18nMessageUtil.get("i18n.auth_directory_cannot_contain_hierarchy.d6ca") + error);
        //
        List<String> staticDirList = AgentWhitelist.parseToList(staticDir, false, I18nMessageUtil.get("i18n.static_directory_auth_cannot_be_empty.2cb2"));
        staticDirList = AgentWhitelist.covertToArray(staticDirList, 100, I18nMessageUtil.get("i18n.static_directory_auth_cannot_be_under_jpom.8879"));
        error = AgentWhitelist.findStartsWith(staticDirList);
        Assert.isNull(error, I18nMessageUtil.get("i18n.static_directory_cannot_contain_relation.1a90") + error);

        ServerWhitelist serverWhitelist = outGivingWhitelistService.getServerWhitelistData(workspaceId);
        serverWhitelist.setOutGiving(list);
        serverWhitelist.setStaticDir(staticDirList);
        //
        List<String> allowRemoteDownloadHostList = AgentWhitelist.parseToList(allowRemoteDownloadHost, I18nMessageUtil.get("i18n.remote_download_host_cannot_be_empty.cdf5"));
        //
        if (CollUtil.isNotEmpty(allowRemoteDownloadHostList)) {
            for (String s : allowRemoteDownloadHostList) {
                Assert.state(ReUtil.isMatch(RegexPool.URL_HTTP, s), I18nMessageUtil.get("i18n.invalid_remote_address_format.7f32") + s);
            }
        }
        serverWhitelist.setAllowRemoteDownloadHost(allowRemoteDownloadHostList == null ? null : CollUtil.newHashSet(allowRemoteDownloadHostList));
        //

        String id = ServerWhitelist.workspaceId(workspaceId);
        systemParametersServer.upsert(id, serverWhitelist, id);

        String resultData = AgentWhitelist.convertToLine(list);
        // 重新检查静态目录任务状态
        I18nThreadUtil.execute(() -> {
            try {
                staticFileStorageService.startLoad();
            } catch (Exception e) {
                log.error(I18nMessageUtil.get("i18n.static_file_task_load_failure.b995"), e);
            }
        });

        return JsonMessage.success(I18nMessageUtil.get("i18n.save_succeeded.3b10"), resultData);
    }
}
