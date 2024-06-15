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

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.common.BaseAgentController;
import org.dromara.jpom.common.Const;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.system.ExtConfigBean;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 系统配置
 *
 * @author bwcx_jzy
 * @since 2019/08/08
 */
@RestController
@RequestMapping(value = "system")
@Slf4j
public class SystemConfigController extends BaseAgentController {

    @RequestMapping(value = "getConfig.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<JSONObject> config() throws IOException {
        Resource resource = ExtConfigBean.getResource();
        String content = IoUtil.read(resource.getInputStream(), CharsetUtil.CHARSET_UTF_8);
        JSONObject json = new JSONObject();
        json.put("content", content);
        json.put("file", FileUtil.getAbsolutePath(resource.getFile()));
        return JsonMessage.success("", json);
    }

    @RequestMapping(value = "save_config.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<String> saveConfig(@ValidatorItem(msg = "i18n.content_cannot_be_empty.9f0d") String content, String restart) throws IOException {
        try {
            YamlPropertySourceLoader yamlPropertySourceLoader = new YamlPropertySourceLoader();
            // @author hjk 前端编辑器允许使用tab键，并设定为2个空格，再转换为yml时要把tab键换成2个空格
            ByteArrayResource resource = new ByteArrayResource(content.replace("\t", "  ").getBytes(StandardCharsets.UTF_8));
            yamlPropertySourceLoader.load("test", resource);
        } catch (Exception e) {
            log.warn(I18nMessageUtil.get("i18n.content_format_error.ce15"), e);
            return new JsonMessage<>(500, I18nMessageUtil.get("i18n.content_format_error_with_detail.c846") + e.getMessage());
        }
        Resource resource = ExtConfigBean.getResource();
        Assert.state(resource.isFile(), I18nMessageUtil.get("i18n.configuration_modification_not_supported.5872"));
        FileUtil.writeString(content, resource.getFile(), CharsetUtil.CHARSET_UTF_8);

        if (Convert.toBool(restart, false)) {
            // 重启
            JpomApplication.restart();
            return JsonMessage.success(Const.UPGRADE_MSG.get());
        }
        return JsonMessage.success(I18nMessageUtil.get("i18n.modify_success.69be"));
    }
}
