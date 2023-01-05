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

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.net.Ipv4Util;
import cn.hutool.core.net.MaskBit;
import cn.hutool.core.text.CharPool;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import io.jpom.JpomApplication;
import io.jpom.common.BaseServerController;
import io.jpom.common.Const;
import io.jpom.common.JsonMessage;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.common.validator.ValidatorItem;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.SystemIpConfigModel;
import io.jpom.model.node.NodeAgentWhitelist;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.permission.SystemPermission;
import io.jpom.service.node.NodeService;
import io.jpom.service.system.SystemParametersServer;
import io.jpom.system.ExtConfigBean;
import io.jpom.system.db.InitDb;
import io.jpom.system.extconf.DbExtConfig;
import io.jpom.system.init.ProxySelectorConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.YamlMapFactoryBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
    private final DbExtConfig dbExtConfig;

    public SystemConfigController(SystemParametersServer systemParametersServer,
                                  InitDb initDb,
                                  NodeService nodeService,
                                  ProxySelectorConfig proxySelectorConfig,
                                  DbExtConfig dbExtConfig) {
        this.systemParametersServer = systemParametersServer;
        this.initDb = initDb;
        this.nodeService = nodeService;
        this.proxySelectorConfig = proxySelectorConfig;
        this.dbExtConfig = dbExtConfig;
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
    public JsonMessage<JSONObject> configData(String nodeId) throws IOException {
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

        return JsonMessage.success("", jsonObject);
    }

    @PostMapping(value = "save_config.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    @SystemPermission(superUser = true)
    public JsonMessage<String> saveConfig(String nodeId, String content, String restart) throws SQLException, IOException {
        if (StrUtil.isNotEmpty(nodeId)) {
            return NodeForward.request(getNode(), getRequest(), NodeUrl.SystemSaveConfig);
        }
        Assert.hasText(content, "内容不能为空");

        ByteArrayResource byteArrayResource;
        try {
            YamlPropertySourceLoader yamlPropertySourceLoader = new YamlPropertySourceLoader();
            // @author hjk 前端编辑器允许使用tab键，并设定为2个空格，再转换为yml时要把tab键换成2个空格
            byteArrayResource = new ByteArrayResource(content.replace("\t", "  ").getBytes(StandardCharsets.UTF_8));
            yamlPropertySourceLoader.load("test", byteArrayResource);
        } catch (Exception e) {
            log.warn("内容格式错误，请检查修正", e);
            return new JsonMessage<>(500, "内容格式错误，请检查修正:" + e.getMessage());
        }
        boolean restartBool = Convert.toBool(restart, false);
        // 修改数据库密码
        YamlMapFactoryBean yamlMapFactoryBean = new YamlMapFactoryBean();
        yamlMapFactoryBean.setResources(byteArrayResource);

        Map<String, Object> yamlMap = yamlMapFactoryBean.getObject();
        ConfigurationProperties configurationProperties = DbExtConfig.class.getAnnotation(ConfigurationProperties.class);
        Assert.notNull(configurationProperties, "没有找到数据库配置标识头");
        Map<String, Object> dbYamlMap = BeanUtil.getProperty(yamlMap, configurationProperties.prefix());
        Assert.notNull(dbYamlMap, "未解析出配置文件中的数据库配置信息");
        // 解析字段密码
        DbExtConfig dbExtConfig2 = BeanUtil.toBean(dbYamlMap, DbExtConfig.class, CopyOptions.create()
            .setIgnoreError(true)
            .setFieldNameEditor(s -> {
                String camelCase = StrUtil.toCamelCase(s);
                return StrUtil.toCamelCase(camelCase, CharPool.DASHED);
            }));
        Assert.hasText(dbExtConfig2.getUserName(), "未配置(未解析到)数据库用户名");

        String newDbExtConfigUserName = dbExtConfig2.userName();
        String newDbExtConfigUserPwd = dbExtConfig2.userPwd();
        String oldDbExtConfigUserName = dbExtConfig.userName();
        String oldDbExtConfigUserPwd = dbExtConfig.userPwd();
        if (!StrUtil.equals(oldDbExtConfigUserName, newDbExtConfigUserName) || !StrUtil.equals(oldDbExtConfigUserPwd, newDbExtConfigUserPwd)) {
            // 执行修改数据库账号密码
            Assert.state(restartBool, "修改数据库密码必须重启");
            initDb.alterUser(oldDbExtConfigUserName, newDbExtConfigUserName, newDbExtConfigUserPwd);
        }
        Resource resource = ExtConfigBean.getResource();
        Assert.state(resource.isFile(), "当前环境下不支持在线修改配置文件");
        FileUtil.writeString(content, resource.getFile(), CharsetUtil.CHARSET_UTF_8);

        if (restartBool) {
            // 重启
            JpomApplication.restart();
            return JsonMessage.success(Const.UPGRADE_MSG);
        }
        return JsonMessage.success("修改成功");
    }


    /**
     * 加载服务端的 ip 白名单配置
     *
     * @return json
     */
    @RequestMapping(value = "ip-config-data", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.SYSTEM_CONFIG_IP, method = MethodFeature.LIST)
    public JsonMessage<JSONObject> ipConfigData() {
        SystemIpConfigModel config = systemParametersServer.getConfig(SystemIpConfigModel.ID, SystemIpConfigModel.class);
        JSONObject jsonObject = new JSONObject();
        if (config != null) {
            jsonObject.put("allowed", config.getAllowed());
            jsonObject.put("prohibited", config.getProhibited());
        }
        //jsonObject.put("path", FileUtil.getAbsolutePath(systemIpConfigService.filePath()));
        jsonObject.put("ip", getIp());
        return JsonMessage.success("加载成功", jsonObject);
    }

    @RequestMapping(value = "save_ip_config.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.SYSTEM_CONFIG_IP, method = MethodFeature.EDIT)
    public JsonMessage<Object> saveIpConfig(String allowed, String prohibited) {
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
        return JsonMessage.success("修改成功");
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
    public JsonMessage<NodeAgentWhitelist> getWhitelist() {
        String workspaceId = nodeService.getCheckUserWorkspace(getRequest());
        NodeAgentWhitelist config = systemParametersServer.getConfigDefNewInstance(StrUtil.format("node_whitelist_{}", workspaceId), NodeAgentWhitelist.class);
        return JsonMessage.success("加载成功", config);
    }

    /**
     * 保存白名单配置
     *
     * @return json
     */
    @RequestMapping(value = "save_whitelist", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.SYSTEM_NODE_WHITELIST, method = MethodFeature.EDIT)
    public JsonMessage<Object> saveWhitelist(@ValidatorItem(msg = "请选择分发的节点") String nodeIds,
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
        for (String s : nodeIdsStr) {
            NodeModel byKey = nodeService.getByKey(s, httpServletRequest);
            JSONObject jsonObject = (JSONObject) JSON.toJSON(agentWhitelist);
            JsonMessage<String> request = NodeForward.request(byKey, NodeUrl.WhitelistDirectory_Submit, jsonObject);
            Assert.state(request.getCode() == 200, "分发 " + byKey.getName() + " 节点配置失败" + request.getMsg());
        }
        return JsonMessage.success("保存成功");
    }

    /**
     * 加载节点配置
     *
     * @return json
     */
    @RequestMapping(value = "get_node_config", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public JsonMessage<JSONObject> getNodeConfig() {
        String workspaceId = nodeService.getCheckUserWorkspace(getRequest());
        JSONObject config = systemParametersServer.getConfigDefNewInstance(StrUtil.format("node_config_{}", workspaceId), JSONObject.class);
        return JsonMessage.success("", config);
    }

    @PostMapping(value = "save_node_config.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    @SystemPermission(superUser = true)
    public JsonMessage<Object> saveNodeConfig(@ValidatorItem(msg = "请选择分发的节点") String nodeIds, String templateNodeId, String content, String restart) {
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
        for (String s : nodeIdsStr) {
            NodeModel byKey = nodeService.getByKey(s, httpServletRequest);
            JSONObject reqData = new JSONObject();
            reqData.put("content", content);
            reqData.put("restart", restart);
            JsonMessage<String> request = NodeForward.request(byKey, NodeUrl.SystemSaveConfig, reqData);
            Assert.state(request.getCode() == 200, "分发 " + byKey.getName() + " 节点配置失败" + request.getMsg());
        }
        return JsonMessage.success("修改成功");
    }

    /**
     * 加载菜单配置
     *
     * @return json
     */
    @RequestMapping(value = "get_menus_config", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public JsonMessage<JSONObject> getMenusConfig() {
        String workspaceId = nodeService.getCheckUserWorkspace(getRequest());
        JSONObject config = systemParametersServer.getConfigDefNewInstance(StrUtil.format("menus_config_{}", workspaceId), JSONObject.class);
        //"classpath:/menus/index.json"
        //"classpath:/menus/node-index.json"
        config.put("serverMenus", this.readMenusJson("classpath:/menus/index.json"));
        config.put("nodeMenus", this.readMenusJson("classpath:/menus/node-index.json"));
        return JsonMessage.success("", config);
    }

    @PostMapping(value = "save_menus_config.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.SYSTEM_CONFIG_MENUS, method = MethodFeature.EDIT)
    public JsonMessage<Object> saveMenusConfig(String serverMenuKeys, String nodeMenuKeys) {
        String workspaceId = nodeService.getCheckUserWorkspace(getRequest());
        //
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("nodeMenuKeys", StrUtil.splitTrim(nodeMenuKeys, StrUtil.COMMA));
        jsonObject.put("serverMenuKeys", StrUtil.splitTrim(serverMenuKeys, StrUtil.COMMA));
        String format = StrUtil.format("menus_config_{}", workspaceId);
        systemParametersServer.upsert(format, jsonObject, format);
        //
        return JsonMessage.success("修改成功");
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
    public JsonMessage<JSONArray> getPoxyConfig() {
        JSONArray array = systemParametersServer.getConfigDefNewInstance(ProxySelectorConfig.KEY, JSONArray.class);
        return JsonMessage.success("", array);
    }

    /**
     * 保存代理
     *
     * @param proxys 参数
     * @return json
     */
    @PostMapping(value = "save_proxy_config", produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonMessage<Object> saveProxyConfig(@RequestBody List<ProxySelectorConfig.ProxyConfigItem> proxys) {
        proxys = ObjectUtil.defaultIfNull(proxys, Collections.emptyList());
        for (ProxySelectorConfig.ProxyConfigItem proxy : proxys) {
            if (StrUtil.isNotEmpty(proxy.getProxyAddress())) {
                nodeService.testHttpProxy(proxy.getProxyAddress());
            }
        }
        systemParametersServer.upsert(ProxySelectorConfig.KEY, proxys, ProxySelectorConfig.KEY);
        proxySelectorConfig.refresh();
        return JsonMessage.success("修改成功");
    }

}
