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
package io.jpom.controller.openapi;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.ServerOpenApi;
import io.jpom.system.ConfigBean;
import io.jpom.system.ServerConfigBean;
import io.jpom.util.JsonFileUtil;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * 获取当前服务端安装id
 *
 * @author bwcx_jzy
 * @since 2019/8/7
 */
@RestController
public class InstallIdController {

    @RequestMapping(value = ServerOpenApi.INSTALL_ID, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String install() throws FileNotFoundException {
        File file = FileUtil.file(ConfigBean.getInstance().getDataPath(), ServerConfigBean.INSTALL);
        if (!file.exists()) {
            return JsonMessage.getString(500, "服务端的安装信息文件不存在");
        }
        JSONObject json = (JSONObject) JsonFileUtil.readJson(file.getAbsolutePath());
        String installId = json.getString("installId");
        if (StrUtil.isEmpty(installId)) {
            return JsonMessage.getString(500, "服务端的安装Id为空");
        }
        return JsonMessage.getString(200, "ok", installId);
    }
}
