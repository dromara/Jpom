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
package io.jpom.system.init;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson2.JSONObject;
import io.jpom.JpomApplication;
import io.jpom.common.ILoadEvent;
import io.jpom.common.ServerConst;
import io.jpom.util.JsonFileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

import java.io.File;

/**
 * 自动导入本机节点
 *
 * @author jiangzeyin
 * @since 2019/4/18
 */
@Configuration
@Slf4j
public class AutoImportLocalNode implements ILoadEvent {

    private final JpomApplication configBean;

    public AutoImportLocalNode(JpomApplication configBean) {
        this.configBean = configBean;
    }

    private void install() {
        File file = FileUtil.file(configBean.getDataPath(), ServerConst.INSTALL);
        if (file.exists()) {
            return;
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("installId", IdUtil.fastSimpleUUID());
        jsonObject.put("installTime", DateTime.now().toString());
        jsonObject.put("desc", "请勿删除此文件,服务端安装id和插件端互通关联");
        JsonFileUtil.saveJson(file.getAbsolutePath(), jsonObject);
    }


    @Override
    public void afterPropertiesSet(ApplicationContext applicationContext) throws Exception {
        this.install();
    }
}
