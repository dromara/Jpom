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
package org.dromara.jpom.controller.system;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.io.FileUtil;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.build.BuildInfoManage;
import org.dromara.jpom.build.BuildUtil;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.ICacheTask;
import org.dromara.jpom.common.JpomManifest;
import org.dromara.jpom.common.JsonMessage;
import org.dromara.jpom.common.forward.NodeForward;
import org.dromara.jpom.common.forward.NodeUrl;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.common.validator.ValidatorRule;
import org.dromara.jpom.controller.LoginControl;
import org.dromara.jpom.cron.CronUtils;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.permission.SystemPermission;
import org.dromara.jpom.plugin.PluginFactory;
import org.dromara.jpom.socket.ServiceFileTailWatcher;
import org.dromara.jpom.system.db.DataInitEvent;
import org.dromara.jpom.util.CommandUtil;
import org.dromara.jpom.util.SyncFinisherUtil;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

/**
 * 缓存管理
 *
 * @author bwcx_jzy
 * @since 2019/7/20
 */
@RestController
@RequestMapping(value = "system")
@Feature(cls = ClassFeature.SYSTEM_CACHE)
@SystemPermission
public class CacheManageController extends BaseServerController implements ICacheTask {

    private long dataSize;
    private long oldJarsSize;
    private long tempFileSize;

    private final JpomApplication jpomApplication;
    private final DataInitEvent dataInitEvent;

    public CacheManageController(JpomApplication jpomApplication,
                                 DataInitEvent dataInitEvent) {
        this.jpomApplication = jpomApplication;
        this.dataInitEvent = dataInitEvent;
    }

    /**
     * get server's cache data
     * 获取 Server 的缓存数据
     *
     * @return json
     * @author Hotstrip
     */
    @PostMapping(value = "server-cache", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public JsonMessage<Map<String, Object>> serverCache() {
        Map<String, Object> map = new HashMap<>(10);
        map.put("cacheFileSize", this.tempFileSize);
        map.put("dataSize", this.dataSize);
        int size = LoginControl.LFU_CACHE.size();
        map.put("oldJarsSize", this.oldJarsSize);
        map.put("ipSize", size);
        int oneLineCount = ServiceFileTailWatcher.getOneLineCount();
        map.put("readFileOnLineCount", oneLineCount);

        map.put("cacheBuildFileSize", BuildUtil.buildCacheSize);

        map.put("taskList", CronUtils.list());
        map.put("pluginSize", PluginFactory.size());
        map.put("shardingSize", BaseServerController.SHARDING_IDS.size());
        map.put("buildKeys", BuildInfoManage.buildKeys());
        map.put("syncFinisKeys", SyncFinisherUtil.keys());
        map.put("dateTime", DateTime.now().toString());
        map.put("timeZoneId", TimeZone.getDefault().getID());
        map.put("errorWorkspace", dataInitEvent.getErrorWorkspaceTable());
        //
        return JsonMessage.success("ok", map);
    }

    /**
     * 获取节点中的缓存
     *
     * @return json
     */
    @RequestMapping(value = "node_cache.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public String nodeCache() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.Cache).toString();
    }

    /**
     * 清空缓存
     *
     * @param type 类型
     * @return json
     */
    @RequestMapping(value = "clearCache.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public JsonMessage<String> clearCache(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "类型错误") String type) {
        switch (type) {
            case "serviceCacheFileSize": {
                File tempPath = JpomApplication.getInstance().getTempPath();
                boolean clean = CommandUtil.systemFastDel(tempPath);
                Assert.state(!clean, "清空文件缓存失败");
                break;
            }
            case "serviceIpSize":
                LoginControl.LFU_CACHE.clear();
                break;
            case "serviceOldJarsSize": {
                File oldJarsPath = JpomManifest.getOldJarsPath();
                boolean clean = CommandUtil.systemFastDel(oldJarsPath);
                Assert.state(!clean, "清空旧版本重新包失败");
                break;
            }
            default:
                return NodeForward.request(getNode(), getRequest(), NodeUrl.ClearCache);

        }
        return JsonMessage.success("清空成功");
    }

    /**
     * 清理错误的工作空间数据
     *
     * @param tableName 类型
     * @return json
     */
    @GetMapping(value = "clear-error-workspace", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public JsonMessage<String> clearErrorWorkspace(@ValidatorItem String tableName) {
        dataInitEvent.clearErrorWorkspace(tableName);
        return JsonMessage.success("清理成功");
    }

    @Override
    public void refreshCache() {
        File file = jpomApplication.getTempPath();
        this.tempFileSize = FileUtil.size(file);
        this.dataSize = jpomApplication.dataSize();
        File oldJarsPath = JpomManifest.getOldJarsPath();
        this.oldJarsSize = FileUtil.size(oldJarsPath);
        BuildUtil.reloadCacheSize();
    }
}
