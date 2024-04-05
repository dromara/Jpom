/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.func.system.controller;

import cn.hutool.cache.impl.CacheObj;
import cn.hutool.cache.impl.LFUCache;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.event.ICacheTask;
import cn.keepbx.jpom.event.ISystemTask;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.build.BuildExecuteManage;
import org.dromara.jpom.build.BuildUtil;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.JpomManifest;
import org.dromara.jpom.common.forward.NodeUrl;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.common.validator.ValidatorRule;
import org.dromara.jpom.configuration.ClusterConfig;
import org.dromara.jpom.configuration.SystemConfig;
import org.dromara.jpom.controller.LoginControl;
import org.dromara.jpom.cron.CronUtils;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.permission.SystemPermission;
import org.dromara.jpom.plugin.PluginFactory;
import org.dromara.jpom.socket.ServiceFileTailWatcher;
import org.dromara.jpom.system.ServerConfig;
import org.dromara.jpom.system.db.DataInitEvent;
import org.dromara.jpom.util.CommandUtil;
import org.dromara.jpom.util.SyncFinisherUtil;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.HashMap;
import java.util.List;
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
@Slf4j
public class CacheManageController extends BaseServerController implements ICacheTask, ISystemTask {

    private long dataSize;
    private long oldJarsSize;
    private long tempFileSize;

    private final JpomApplication jpomApplication;
    private final DataInitEvent dataInitEvent;
    private final ClusterConfig clusterConfig;
    private final SystemConfig systemConfig;
    /**
     * 标记是否正在刷新缓存
     */
    private boolean refreshCacheIng = false;

    public CacheManageController(JpomApplication jpomApplication,
                                 DataInitEvent dataInitEvent,
                                 ServerConfig serverConfig) {
        this.jpomApplication = jpomApplication;
        this.dataInitEvent = dataInitEvent;
        this.clusterConfig = serverConfig.getCluster();
        this.systemConfig = serverConfig.getSystem();
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
    public IJsonMessage<Map<String, Object>> serverCache() {
        Map<String, Object> map = new HashMap<>(10);
        map.put("cacheFileSize", this.tempFileSize);
        map.put("dataSize", this.dataSize);
        map.put("oldJarsSize", this.oldJarsSize);
        {
            LFUCache<String, Integer> lfuCache = LoginControl.LFU_CACHE;
            List<CacheObj<String, Integer>> list = CollUtil.newArrayList(lfuCache.cacheObjIterator());
            map.put("errorIp", list);
        }
        int oneLineCount = ServiceFileTailWatcher.getOneLineCount();
        map.put("readFileOnLineCount", oneLineCount);
        map.put("cacheBuildFileSize", BuildUtil.buildCacheSize);
        map.put("taskList", CronUtils.list());
        map.put("pluginSize", PluginFactory.size());
        map.put("shardingSize", BaseServerController.SHARDING_IDS.size());
        map.put("buildKeys", BuildExecuteManage.buildKeys());
        map.put("syncFinisKeys", SyncFinisherUtil.keys());
        map.put("dateTime", DateTime.now().toString());
        map.put("timeZoneId", TimeZone.getDefault().getID());
        map.put("errorWorkspace", dataInitEvent.getErrorWorkspaceTable());
        map.put("clusterId", clusterConfig.getId());
        JpomManifest jpomManifest = JpomManifest.getInstance();
        map.put("installId", jpomManifest.getInstallId());
        map.put("tempPath", jpomApplication.getTempPath().getAbsolutePath());
        map.put("dataPath", jpomApplication.getDataPath());
        map.put("buildPath", BuildUtil.getBuildDataDir());
        map.put("timerMatchSecond", systemConfig.isTimerMatchSecond());
        //
        return JsonMessage.success("", map);
    }

    /**
     * 获取节点中的缓存
     *
     * @return json
     */
    @RequestMapping(value = "node_cache.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<JSONObject> nodeCache(HttpServletRequest request, @ValidatorItem String machineId) {
        return this.tryRequestMachine(machineId, request, NodeUrl.Cache);

//        return Optional.ofNullable(message).orElseGet(() -> {
//            List<JSONObject> data = DirTreeUtil.getTreeData(LogbackConfig.getPath());
//            return JsonMessage.success("", data);
//        });
//        return NodeForward.request(getNode(), request, NodeUrl.Cache).toString();
    }

    /**
     * 清空缓存
     *
     * @param type 类型
     * @return json
     */
    @RequestMapping(value = "clearCache.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public IJsonMessage<String> clearCache(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "类型错误") String type,
                                           String machineId,
                                           HttpServletRequest request) {
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
                return this.tryRequestMachine(machineId, request, NodeUrl.ClearCache);
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
    public IJsonMessage<String> clearErrorWorkspace(@ValidatorItem String tableName) {
        dataInitEvent.clearErrorWorkspace(tableName);
        return JsonMessage.success("清理成功");
    }

    @GetMapping(value = "async-refresh-cache", produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<String> refresh() {
        Assert.state(!this.refreshCacheIng, "正在刷新缓存中,请勿重复刷新");
        ThreadUtil.execute(() -> {
            try {
                this.refreshCacheIng = true;
                this.executeTask();
            } catch (Exception e) {
                log.error("手动刷新缓存异常", e);
            } finally {
                this.refreshCacheIng = false;
            }
        });
        return JsonMessage.success("异步刷新中请稍后刷新页面查看");
    }

    @Override
    public void refreshCache() {
        File file = jpomApplication.getTempPath();
        this.tempFileSize = FileUtil.size(file);
        File oldJarsPath = JpomManifest.getOldJarsPath();
        this.oldJarsSize = FileUtil.size(oldJarsPath);
    }

    @Override
    public void executeTask() {
        this.dataSize = jpomApplication.dataSize();
        BuildUtil.reloadCacheSize();
    }
}
