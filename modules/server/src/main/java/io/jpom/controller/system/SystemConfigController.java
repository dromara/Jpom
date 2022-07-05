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
package io.jpom.controller.system;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.net.Ipv4Util;
import cn.hutool.core.net.MaskBit;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.jpom.JpomApplication;
import io.jpom.common.BaseServerController;
import io.jpom.common.Const;
import io.jpom.common.JpomManifest;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.SystemIpConfigModel;
import io.jpom.model.data.UserModel;
import io.jpom.model.node.NodeAgentWhitelist;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.permission.SystemPermission;
import io.jpom.service.node.NodeService;
import io.jpom.service.system.SystemParametersServer;
import io.jpom.system.ExtConfigBean;
import io.jpom.system.extconf.DbExtConfig;
import io.jpom.system.init.InitDb;
import io.jpom.system.init.ProxySelectorConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/**
 * 系统配置
 *
 * @author bwcx_jzy
 * @since 2019/08/08
 */
@RestController
@RequestMapping(value = "system")
@Feature(cls = ClassFeature.SYSTEM_CONFIG)
@SystemPermission
@Slf4j
public class SystemConfigController extends BaseServerController {

    private final SystemParametersServer systemParametersServer;
    private final InitDb initDb;
    private final NodeService nodeService;
    private final ProxySelectorConfig proxySelectorConfig;

    public SystemConfigController(SystemParametersServer systemParametersServer,
                                  InitDb initDb,
                                  NodeService nodeService,
                                  ProxySelectorConfig proxySelectorConfig) {
        this.systemParametersServer = systemParametersServer;
        this.initDb = initDb;
        this.nodeService = nodeService;
        this.proxySelectorConfig = proxySelectorConfig;
    }

    /**
     * get server's config or node's config
     * 加载服务端或者节点端配置
     *
     * @param nodeId 节点ID
     * @return json
     * @throws IOException io
     * @author Hotstrip
     */
    @RequestMapping(value = "config-data", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public String configData(String nodeId) throws IOException {
        JSONObject jsonObject;
        if (StrUtil.isNotEmpty(nodeId)) {
            jsonObject = NodeForward.requestData(getNode(), NodeUrl.SystemGetConfig, getRequest(), JSONObject.class);
        } else {
            jsonObject = new JSONObject();
            Resource resource = ExtConfigBean.getResource();
            String content = IoUtil.read(resource.getInputStream(), CharsetUtil.CHARSET_UTF_8);
            jsonObject.put("content", content);
            jsonObject.put("file", FileUtil.getAbsolutePath(resource.getFile()));
        }

        return JsonMessage.getString(200, "", jsonObject);
    }

    @PostMapping(value = "save_config.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    @SystemPermission(superUser = true)
    public String saveConfig(String nodeId, String content, String restart) throws IOException, SQLException {
        if (StrUtil.isNotEmpty(nodeId)) {
            return NodeForward.request(getNode(), getRequest(), NodeUrl.SystemSaveConfig).toString();
        }
        Assert.hasText(content, "内容不能为空");
        try {
            YamlPropertySourceLoader yamlPropertySourceLoader = new YamlPropertySourceLoader();
            // @author hjk 前端编辑器允许使用tab键，并设定为2个空格，再转换为yml时要把tab键换成2个空格
            ByteArrayResource resource = new ByteArrayResource(content.replace("\t", "  ").getBytes(StandardCharsets.UTF_8));
            yamlPropertySourceLoader.load("test", resource);
        } catch (Exception e) {
            log.warn("内容格式错误，请检查修正", e);
            return JsonMessage.getString(500, "内容格式错误，请检查修正:" + e.getMessage());
        }
        boolean restartBool = Convert.toBool(restart, false);
        // 修改数据库密码
        DbExtConfig oldDbExtConfig = DbExtConfig.parse(ExtConfigBean.getResource().getInputStream());
        DbExtConfig newDbExtConfig = DbExtConfig.parse(content);
        if (!StrUtil.equals(oldDbExtConfig.getUserName(), newDbExtConfig.getUserName()) || !StrUtil.equals(oldDbExtConfig.getUserPwd(), newDbExtConfig.getUserPwd())) {
            // 执行修改数据库账号密码
            Assert.state(restartBool, "修改数据库密码必须重启");
            initDb.alterUser(oldDbExtConfig.getUserName(), newDbExtConfig.getUserName(), newDbExtConfig.getUserPwd());
        }
        Assert.state(!JpomManifest.getInstance().isDebug(), "调试模式下不支持在线修改,请到resources目录下的bin目录修改extConfig.yml");

        File resourceFile = ExtConfigBean.getResourceFile();
        FileUtil.writeString(content, resourceFile, CharsetUtil.CHARSET_UTF_8);

        if (restartBool) {
            // 重启
            ThreadUtil.execute(() -> {
                ThreadUtil.sleep(2000);
                JpomApplication.restart();
            });
            return JsonMessage.getString(200, Const.UPGRADE_MSG);
        }
        return JsonMessage.getString(200, "修改成功");
    }


    /**
     * 加载服务端的 ip 白名单配置
     *
     * @return json
     */
    @RequestMapping(value = "ip-config-data", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.SYSTEM_CONFIG_IP, method = MethodFeature.LIST)
    public String ipConfigData() {
        SystemIpConfigModel config = systemParametersServer.getConfig(SystemIpConfigModel.ID, SystemIpConfigModel.class);
        JSONObject jsonObject = new JSONObject();
        if (config != null) {
            jsonObject.put("allowed", config.getAllowed());
            jsonObject.put("prohibited", config.getProhibited());
        }
        //jsonObject.put("path", FileUtil.getAbsolutePath(systemIpConfigService.filePath()));
        jsonObject.put("ip", getIp());
        return JsonMessage.getString(200, "加载成功", jsonObject);
    }

    @RequestMapping(value = "save_ip_config.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.SYSTEM_CONFIG_IP, method = MethodFeature.EDIT)
    public String saveIpConfig(String allowed, String prohibited) {
        SystemIpConfigModel systemIpConfigModel = new SystemIpConfigModel();
        String allowed1 = StrUtil.emptyToDefault(allowed, StrUtil.EMPTY);
        this.checkIpV4(allowed1);
        systemIpConfigModel.setAllowed(allowed1);
        //
        String prohibited1 = StrUtil.emptyToDefault(prohibited, StrUtil.EMPTY);
        systemIpConfigModel.setProhibited(prohibited1);
        this.checkIpV4(prohibited1);
        systemParametersServer.upsert(SystemIpConfigModel.ID, systemIpConfigModel, SystemIpConfigModel.ID);
        //
        return JsonMessage.getString(200, "修改成功");
    }

    /**
     * 检查是否为 ipv4
     *
     * @param ips ip
     */
    private void checkIpV4(String ips) {
        if (StrUtil.isEmpty(ips)) {
            return;
        }
        String[] split = StrUtil.splitToArray(ips, StrUtil.LF);
        for (String itemIp : split) {
            itemIp = itemIp.trim();
            if (itemIp.startsWith("#")) {
                continue;
            }
            if (StrUtil.equals(itemIp, "0.0.0.0")) {
                // 开放所有
                continue;
            }
            if (StrUtil.contains(itemIp, Ipv4Util.IP_MASK_SPLIT_MARK)) {
                String[] param = StrUtil.splitToArray(itemIp, Ipv4Util.IP_MASK_SPLIT_MARK);
                Assert.state(Validator.isIpv4(param[0]), "请填写 ipv4 地址：" + itemIp);
                int count1 = StrUtil.count(param[0], StrUtil.DOT);
                int count2 = StrUtil.count(param[1], StrUtil.DOT);
                if (count1 == 3 && count2 == 3) {
                    //192.168.1.0/192.168.1.200
                    Assert.state(Validator.isIpv4(param[1]), "请填写 ipv4 地址：" + itemIp);
                    continue;
                }
                if (count1 == 3 && count2 == 0) {
                    //192.168.1.0/24
                    int maskBit = Convert.toInt(param[1], 0);
                    String s = MaskBit.get(maskBit);
                    Assert.hasText(s, "子掩码不正确：" + itemIp);
                    continue;
                }
            }
            boolean ipv4 = Validator.isIpv4(itemIp);
            Assert.state(ipv4, "请填写 ipv4 地址：" + itemIp);
        }
    }

    /**
     * 加载白名单配置
     *
     * @return json
     */
    @RequestMapping(value = "get_whitelist", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.SYSTEM_NODE_WHITELIST, method = MethodFeature.LIST)
    public String getWhitelist() {
        String workspaceId = nodeService.getCheckUserWorkspace(getRequest());
        NodeAgentWhitelist config = systemParametersServer.getConfigDefNewInstance(StrUtil.format("node_whitelist_{}", workspaceId), NodeAgentWhitelist.class);
        return JsonMessage.getString(200, "加载成功", config);
    }

    /**
     * 保存白名单配置
     *
     * @return json
     */
    @RequestMapping(value = "save_whitelist", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.SYSTEM_NODE_WHITELIST, method = MethodFeature.EDIT)
    public String saveWhitelist(@ValidatorItem(msg = "请选择分发的节点") String nodeIds,
                                String project,
                                String certificate,
                                String nginx,
                                String allowEditSuffix,
                                String allowRemoteDownloadHost) {
        HttpServletRequest httpServletRequest = getRequest();
        String workspaceId = nodeService.getCheckUserWorkspace(httpServletRequest);
        NodeAgentWhitelist agentWhitelist = new NodeAgentWhitelist();
        agentWhitelist.setNodeIds(nodeIds);
        agentWhitelist.setProject(project);
        agentWhitelist.setCertificate(certificate);
        agentWhitelist.setNginx(nginx);
        agentWhitelist.setAllowEditSuffix(allowEditSuffix);
        agentWhitelist.setAllowRemoteDownloadHost(allowRemoteDownloadHost);
        String format = StrUtil.format("node_whitelist_{}", workspaceId);
        systemParametersServer.upsert(format, agentWhitelist, format);
        //
        List<String> nodeIdsStr = StrUtil.splitTrim(nodeIds, StrUtil.COMMA);
        UserModel user = getUser();
        for (String s : nodeIdsStr) {
            NodeModel byKey = nodeService.getByKey(s, httpServletRequest);
            JSONObject jsonObject = (JSONObject) JSONObject.toJSON(agentWhitelist);
            JsonMessage<String> request = NodeForward.request(byKey, NodeUrl.WhitelistDirectory_Submit, user, jsonObject);
            Assert.state(request.getCode() == 200, "分发 " + byKey.getName() + " 节点配置失败" + request.getMsg());
        }
        return JsonMessage.getString(200, "保存成功");
    }

    /**
     * 加载节点配置
     *
     * @return json
     */
    @RequestMapping(value = "get_node_config", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public String getNodeConfig() {
        String workspaceId = nodeService.getCheckUserWorkspace(getRequest());
        JSONObject config = systemParametersServer.getConfigDefNewInstance(StrUtil.format("node_config_{}", workspaceId), JSONObject.class);
        return JsonMessage.getString(200, "", config);
    }

    @PostMapping(value = "save_node_config.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    @SystemPermission(superUser = true)
    public String saveNodeConfig(@ValidatorItem(msg = "请选择分发的节点") String nodeIds, String templateNodeId, String content, String restart) {
        Assert.hasText(content, "内容不能为空");
        HttpServletRequest httpServletRequest = getRequest();
        String workspaceId = nodeService.getCheckUserWorkspace(httpServletRequest);
        String id = StrUtil.format("node_config_{}", workspaceId);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("templateNodeId", templateNodeId);
        jsonObject.put("nodeIds", nodeIds);
        systemParametersServer.upsert(id, jsonObject, id);
        //
        List<String> nodeIdsStr = StrUtil.splitTrim(nodeIds, StrUtil.COMMA);
        UserModel user = getUser();
        for (String s : nodeIdsStr) {
            NodeModel byKey = nodeService.getByKey(s, httpServletRequest);
            JSONObject reqData = new JSONObject();
            reqData.put("content", content);
            reqData.put("restart", restart);
            JsonMessage<String> request = NodeForward.request(byKey, NodeUrl.SystemSaveConfig, user, reqData);
            Assert.state(request.getCode() == 200, "分发 " + byKey.getName() + " 节点配置失败" + request.getMsg());
        }
        return JsonMessage.getString(200, "修改成功");
    }

    /**
     * 加载菜单配置
     *
     * @return json
     */
    @RequestMapping(value = "get_menus_config", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public String getMenusConfig() {
        String workspaceId = nodeService.getCheckUserWorkspace(getRequest());
        JSONObject config = systemParametersServer.getConfigDefNewInstance(StrUtil.format("menus_config_{}", workspaceId), JSONObject.class);
        //"classpath:/menus/index.json"
        //"classpath:/menus/node-index.json"
        config.put("serverMenus", this.readMenusJson("classpath:/menus/index.json"));
        config.put("nodeMenus", this.readMenusJson("classpath:/menus/node-index.json"));
        return JsonMessage.getString(200, "", config);
    }

    @PostMapping(value = "save_menus_config.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.SYSTEM_CONFIG_MENUS, method = MethodFeature.EDIT)
    public String saveMenusConfig(String serverMenuKeys, String nodeMenuKeys) {
        String workspaceId = nodeService.getCheckUserWorkspace(getRequest());
        //
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("nodeMenuKeys", StrUtil.splitTrim(nodeMenuKeys, StrUtil.COMMA));
        jsonObject.put("serverMenuKeys", StrUtil.splitTrim(serverMenuKeys, StrUtil.COMMA));
        String format = StrUtil.format("menus_config_{}", workspaceId);
        systemParametersServer.upsert(format, jsonObject, format);
        //
        return JsonMessage.getString(200, "修改成功");
    }

    private JSONArray readMenusJson(String path) {
        // 菜单
        InputStream inputStream = ResourceUtil.getStream(path);
        String json = IoUtil.read(inputStream, CharsetUtil.CHARSET_UTF_8);
        return JSONArray.parseArray(json);
    }

    /**
     * 加载代理配置
     *
     * @return json
     */
    @GetMapping(value = "get_proxy_config", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public String getPoxyConfig() {
        JSONArray array = systemParametersServer.getConfigDefNewInstance(ProxySelectorConfig.KEY, JSONArray.class);
        return JsonMessage.getString(200, "", array);
    }

    /**
     * 保存代理
     *
     * @param proxys 参数
     * @return json
     */
    @PostMapping(value = "save_proxy_config", produces = MediaType.APPLICATION_JSON_VALUE)
    public String saveProxyConfig(@RequestBody List<ProxySelectorConfig.ProxyConfigItem> proxys) {
        proxys = ObjectUtil.defaultIfNull(proxys, Collections.emptyList());
        for (ProxySelectorConfig.ProxyConfigItem proxy : proxys) {
            if (StrUtil.isNotEmpty(proxy.getProxyAddress())) {
                nodeService.testHttpProxy(proxy.getProxyAddress());
            }
        }
        systemParametersServer.upsert(ProxySelectorConfig.KEY, proxys, ProxySelectorConfig.KEY);
        proxySelectorConfig.refresh();
        return JsonMessage.getString(200, "修改成功");
    }

}
