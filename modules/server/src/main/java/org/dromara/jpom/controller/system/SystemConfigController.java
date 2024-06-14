/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.controller.system;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.net.Ipv4Util;
import cn.hutool.core.net.MaskBit;
import cn.hutool.core.text.CharPool;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.Const;
import org.dromara.jpom.common.forward.NodeUrl;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.db.DbExtConfig;
import org.dromara.jpom.db.StorageServiceFactory;
import org.dromara.jpom.model.data.SystemIpConfigModel;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.permission.SystemPermission;
import org.dromara.jpom.service.system.SystemParametersServer;
import org.dromara.jpom.system.ExtConfigBean;
import org.dromara.jpom.system.init.ProxySelectorConfig;
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
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    private final ProxySelectorConfig proxySelectorConfig;
    private final DbExtConfig dbExtConfig;

    public SystemConfigController(SystemParametersServer systemParametersServer,
                                  ProxySelectorConfig proxySelectorConfig,
                                  DbExtConfig dbExtConfig) {
        this.systemParametersServer = systemParametersServer;
        this.proxySelectorConfig = proxySelectorConfig;
        this.dbExtConfig = dbExtConfig;
    }

    /**
     * get server's config or node's config
     * 加载服务端或者节点端配置
     *
     * @param machineId 机器ID
     * @return json
     * @author Hotstrip
     */
    @RequestMapping(value = "config-data", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<JSONObject> configData(String machineId, HttpServletRequest request) {
        IJsonMessage<JSONObject> message = this.tryRequestMachine(machineId, request, NodeUrl.SystemGetConfig);
        return Optional.ofNullable(message).orElseGet(() -> {
            JSONObject jsonObject = new JSONObject();
            Resource resource = ExtConfigBean.getResource();
            try {
                String content = IoUtil.read(resource.getInputStream(), CharsetUtil.CHARSET_UTF_8);
                jsonObject.put("content", content);
                jsonObject.put("file", FileUtil.getAbsolutePath(resource.getFile()));
                return JsonMessage.success("", jsonObject);
            } catch (IOException e) {
                throw Lombok.sneakyThrow(e);
            }
        });
    }

    @PostMapping(value = "save_config.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    @SystemPermission(superUser = true)
    public IJsonMessage<String> saveConfig(String machineId, String content, String restart, HttpServletRequest request) throws SQLException, IOException {
        JsonMessage<String> jsonMessage = this.tryRequestMachine(machineId, request, NodeUrl.SystemSaveConfig);
        if (jsonMessage != null) {
            return jsonMessage;
        }
        Assert.hasText(content, I18nMessageUtil.get("i18n.content_cannot_be_empty.9f0d"));

        ByteArrayResource byteArrayResource;
        try {
            YamlPropertySourceLoader yamlPropertySourceLoader = new YamlPropertySourceLoader();
            // @author hjk 前端编辑器允许使用tab键，并设定为2个空格，再转换为yml时要把tab键换成2个空格
            byteArrayResource = new ByteArrayResource(content.replace("\t", "  ").getBytes(StandardCharsets.UTF_8));
            yamlPropertySourceLoader.load("test", byteArrayResource);
        } catch (Exception e) {
            log.warn(I18nMessageUtil.get("i18n.content_format_error.ce15"), e);
            return new JsonMessage<>(500, I18nMessageUtil.get("i18n.content_format_error_with_detail.c846") + e.getMessage());
        }
        boolean restartBool = Convert.toBool(restart, false);
        // 修改数据库密码
        YamlMapFactoryBean yamlMapFactoryBean = new YamlMapFactoryBean();
        yamlMapFactoryBean.setResources(byteArrayResource);

        Map<String, Object> yamlMap = yamlMapFactoryBean.getObject();
        ConfigurationProperties configurationProperties = DbExtConfig.class.getAnnotation(ConfigurationProperties.class);
        Assert.notNull(configurationProperties, I18nMessageUtil.get("i18n.no_database_config_header_found.9ee3"));
        Map<String, Object> dbYamlMap = BeanUtil.getProperty(yamlMap, configurationProperties.prefix());
        Assert.notNull(dbYamlMap, I18nMessageUtil.get("i18n.config_file_database_config_not_parsed.47b2"));
        // 解析字段密码
        DbExtConfig dbExtConfig2 = BeanUtil.toBean(dbYamlMap, DbExtConfig.class, CopyOptions.create()
            .setIgnoreError(true)
            .setFieldNameEditor(s -> {
                String camelCase = StrUtil.toCamelCase(s);
                return StrUtil.toCamelCase(camelCase, CharPool.DASHED);
            }));
        Assert.hasText(dbExtConfig2.getUserName(), I18nMessageUtil.get("i18n.database_username_not_configured.a048"));
        if (dbExtConfig2.getMode() == DbExtConfig.Mode.H2) {
            String newDbExtConfigUserName = dbExtConfig2.userName();
            String newDbExtConfigUserPwd = dbExtConfig2.userPwd();
            String oldDbExtConfigUserName = dbExtConfig.userName();
            String oldDbExtConfigUserPwd = dbExtConfig.userPwd();
            if (!StrUtil.equals(oldDbExtConfigUserName, newDbExtConfigUserName) || !StrUtil.equals(oldDbExtConfigUserPwd, newDbExtConfigUserPwd)) {
                // 执行修改数据库账号密码
                Assert.state(restartBool, I18nMessageUtil.get("i18n.modify_db_password_must_restart.d08d"));
                StorageServiceFactory.get().alterUser(oldDbExtConfigUserName, newDbExtConfigUserName, newDbExtConfigUserPwd);
            }
        }
        Resource resource = ExtConfigBean.getResource();
        Assert.state(resource.isFile(), I18nMessageUtil.get("i18n.configuration_modification_not_supported.5872"));
        FileUtil.writeString(content, resource.getFile(), CharsetUtil.CHARSET_UTF_8);

        if (restartBool) {
            // 重启
            JpomApplication.restart();
            return JsonMessage.success(Const.UPGRADE_MSG.get());
        }
        return JsonMessage.success(I18nMessageUtil.get("i18n.modify_success.69be"));
    }


    /**
     * 加载服务端的 ip 授权配置
     *
     * @return json
     */
    @RequestMapping(value = "ip-config-data", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.SYSTEM_CONFIG_IP, method = MethodFeature.LIST)
    public IJsonMessage<JSONObject> ipConfigData() {
        SystemIpConfigModel config = systemParametersServer.getConfig(SystemIpConfigModel.ID, SystemIpConfigModel.class);
        JSONObject jsonObject = new JSONObject();
        if (config != null) {
            jsonObject.put("allowed", config.getAllowed());
            jsonObject.put("prohibited", config.getProhibited());
        }
        //jsonObject.put("path", FileUtil.getAbsolutePath(systemIpConfigService.filePath()));
        jsonObject.put("ip", getIp());
        return JsonMessage.success(I18nMessageUtil.get("i18n.load_success.154e"), jsonObject);
    }

    @RequestMapping(value = "save_ip_config.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.SYSTEM_CONFIG_IP, method = MethodFeature.EDIT)
    public IJsonMessage<Object> saveIpConfig(String allowed, String prohibited) {
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
        return JsonMessage.success(I18nMessageUtil.get("i18n.modify_success.69be"));
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
                Assert.state(Validator.isIpv4(param[0]), I18nMessageUtil.get("i18n.please_fill_in_ipv4_address.d23a") + itemIp);
                int count1 = StrUtil.count(param[0], StrUtil.DOT);
                int count2 = StrUtil.count(param[1], StrUtil.DOT);
                if (count1 == 3 && count2 == 3) {
                    //192.168.1.0/192.168.1.200
                    Assert.state(Validator.isIpv4(param[1]), I18nMessageUtil.get("i18n.please_fill_in_ipv4_address.d23a") + itemIp);
                    continue;
                }
                if (count1 == 3 && count2 == 0) {
                    //192.168.1.0/24
                    int maskBit = Convert.toInt(param[1], 0);
                    String s = MaskBit.get(maskBit);
                    Assert.hasText(s, I18nMessageUtil.get("i18n.subnet_mask_incorrect.6c27") + itemIp);
                    continue;
                }
            }
            boolean ipv4 = Validator.isIpv4(itemIp);
            Assert.state(ipv4, I18nMessageUtil.get("i18n.please_fill_in_ipv4_address.d23a") + itemIp);
        }
    }


    /**
     * 加载代理配置
     *
     * @return json
     */
    @GetMapping(value = "get_proxy_config", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<JSONArray> getPoxyConfig() {
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
    public IJsonMessage<Object> saveProxyConfig(@RequestBody List<ProxySelectorConfig.ProxyConfigItem> proxys) {
        proxys = ObjectUtil.defaultIfNull(proxys, Collections.emptyList());
        for (ProxySelectorConfig.ProxyConfigItem proxy : proxys) {
            if (StrUtil.isNotEmpty(proxy.getProxyAddress())) {
                machineNodeServer.testHttpProxy(proxy.getProxyAddress());
            }
        }
        systemParametersServer.upsert(ProxySelectorConfig.KEY, proxys, ProxySelectorConfig.KEY);
        proxySelectorConfig.refreshCache();
        return JsonMessage.success(I18nMessageUtil.get("i18n.modify_success.69be"));
    }

}
