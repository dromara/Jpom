/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.controller;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;

/**
 * <h1>Jpom 为开源软件，请基于开源协议用于商业用途</h1>
 * <p>
 * 开源不等同于免费，如果您基于 Jpom 二次开发修改了 logo、名称、版权等，请联系我们授权，否则会有法律风险。 我们有权利追诉破坏开源并因此获利的团队个人的全部违法所得，也欢迎给我们提供侵权线索。
 * <p>
 *
 * <h1>二次修改不可删除或者修改版权，否则可能承担法律责任</h1>
 *
 * @author bwcx_jzy
 */
@RestController
@RequestMapping(value = "about")
@Slf4j
public class AboutController {

    /**
     * 擅自修改或者删除版权信息有法律风险，请尊重开源协议，不要擅自修改版本信息，否则可能承担法律责任。
     *
     * @return json
     */
    @GetMapping(value = "license", produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<String> license() {
        InputStream inputStream = ResourceUtil.getStream("classpath:/LICENSE");
        return JsonMessage.success("", IoUtil.readUtf8(inputStream));
    }

    /**
     * 擅自修改或者删除版权信息有法律风险，请尊重开源协议，不要擅自修改版本信息，否则可能承担法律责任。
     * <p>
     * 请严格遵循相关组件开源协议，擅自修改造成的侵权行为，由修改者自行承担全部法律责任。
     *
     * @return json
     */
    @GetMapping(value = "thank-dependency", produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<JSONArray> thankDependency() {
        InputStream inputStream = ResourceUtil.getStream("classpath:/thank-dependency.json");
        String data = IoUtil.readUtf8(inputStream);
        JSONArray jsonArray = JSONArray.parseArray(data);
        jsonArray.sort((o1, o2) -> {
            JSONObject jsonObject1 = (JSONObject) o1;
            JSONObject jsonObject2 = (JSONObject) o2;
            String name = jsonObject1.getString("name");
            String name1 = jsonObject2.getString("name");
            return StrUtil.compareIgnoreCase(name, name1, true);
        });
        return JsonMessage.success("", jsonArray);
    }
}
