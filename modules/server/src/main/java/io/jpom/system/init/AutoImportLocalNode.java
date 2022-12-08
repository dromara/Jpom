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
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.JpomManifest;
import io.jpom.common.Type;
import io.jpom.model.data.NodeModel;
import io.jpom.model.system.AgentAutoUser;
import io.jpom.service.node.NodeService;
import io.jpom.system.ConfigBean;
import io.jpom.system.ServerConfigBean;
import io.jpom.util.JsonFileUtil;
import io.jpom.util.JvmUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
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
public class AutoImportLocalNode implements InitializingBean {

    private final NodeService nodeService;
    private final ConfigBean configBean;

    public AutoImportLocalNode(NodeService nodeService,
                               ConfigBean configBean) {
        this.nodeService = nodeService;
        this.configBean = configBean;
    }


    private void install() {
        File file = FileUtil.file(configBean.getDataPath(), ServerConfigBean.INSTALL);
        if (file.exists()) {
            return;
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("installId", IdUtil.fastSimpleUUID());
        jsonObject.put("installTime", DateTime.now().toString());
        jsonObject.put("desc", "请勿删除此文件,服务端安装id和插件端互通关联");
        JsonFileUtil.saveJson(file.getAbsolutePath(), jsonObject);
    }


    private void loadAgent() {
        long count = nodeService.count();
        if (count > 0) {
            return;
        }
        //
        try {
            Integer mainClassPid = JvmUtil.findMainClassPid(Type.Agent.getApplicationClass());
            if (mainClassPid == null) {
                return;
            }
            findPid(mainClassPid.toString());
        } catch (Exception e) {
            log.error("自动添加本机节点错误", e);
        }
    }

    private void findPid(String pid) {
        File file = configBean.getApplicationJpomInfo(Type.Agent);
        if (!file.exists() || file.isDirectory()) {
            return;
        }
        // 比较进程id
        String json = FileUtil.readString(file, CharsetUtil.CHARSET_UTF_8);
        JpomManifest jpomManifest = JSONObject.parseObject(json, JpomManifest.class);
        if (!pid.equals(String.valueOf(jpomManifest.getPid()))) {
            return;
        }
        // 判断自动授权文件是否存在
        String path = configBean.getAgentAutoAuthorizeFile(jpomManifest.getDataPath());
        if (!FileUtil.exist(path)) {
            return;
        }
        json = FileUtil.readString(path, CharsetUtil.CHARSET_UTF_8);
        AgentAutoUser autoUser = JSONObject.parseObject(json, AgentAutoUser.class);
        // 判断授权信息
        //
        NodeModel nodeModel = new NodeModel();
        nodeModel.setUrl(StrUtil.format("127.0.0.1:{}", jpomManifest.getPort()));
        nodeModel.setName("本机");
        //nodeModel.setProtocol("http");
        //
        nodeModel.setLoginPwd(autoUser.getAgentPwd());
        nodeModel.setLoginName(autoUser.getAgentName());
        //
        nodeModel.setOpenStatus(1);
        nodeService.insertNotFill(nodeModel);
        Console.log("Automatically add native node successfully：" + nodeModel.getId());
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.install();
        this.loadAgent();
    }
}
