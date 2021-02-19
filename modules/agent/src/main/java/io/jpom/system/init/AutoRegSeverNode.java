package io.jpom.system.init;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpStatus;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.PreLoadClass;
import cn.jiangzeyin.common.PreLoadMethod;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.ServerOpenApi;
import io.jpom.system.AgentAuthorize;
import io.jpom.system.AgentConfigBean;
import io.jpom.system.AgentExtConfigBean;
import io.jpom.system.ConfigBean;
import io.jpom.util.JsonFileUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;

/**
 * 自动注册server 节点
 *
 * @author bwcx_jzy
 * @date 2019/8/6
 */
@PreLoadClass
public class AutoRegSeverNode {

    @PreLoadMethod
    private static void reg() throws FileNotFoundException {
        String agentId = AgentExtConfigBean.getInstance().getAgentId();
        String serverUrl = AgentExtConfigBean.getInstance().getServerUrl();
        if (StrUtil.isEmpty(agentId) || StrUtil.isEmpty(serverUrl)) {
            //  如果二者缺一不注册
            return;
        }
        String oldInstallId = null;
        File file = FileUtil.file(ConfigBean.getInstance().getDataPath(), AgentConfigBean.SERVER_ID);
        JSONObject serverJson = null;
        if (file.exists()) {
            serverJson = (JSONObject) JsonFileUtil.readJson(file.getAbsolutePath());
            oldInstallId = serverJson.getString("installId");
        }
        HttpRequest installRequest = AgentExtConfigBean.getInstance().createServerRequest(ServerOpenApi.INSTALL_ID);
        String body1 = installRequest.execute().body();
        JsonMessage jsonMessage = JSON.parseObject(body1, JsonMessage.class);
        if (jsonMessage.getCode() != HttpStatus.HTTP_OK) {
            DefaultSystemLog.getLog().error("获取Server 安装id失败:" + jsonMessage);
            return;
        }
        String installId = jsonMessage.dataToString();
        boolean eqInstall = StrUtil.equals(oldInstallId, installId);
        //
        URL url = URLUtil.toUrlForHttp(AgentExtConfigBean.getInstance().getAgentUrl());
        String protocol = url.getProtocol();

        HttpRequest serverRequest = AgentExtConfigBean.getInstance().createServerRequest(ServerOpenApi.UPDATE_NODE_INFO);
        serverRequest.form("id", agentId);
        serverRequest.form("name", "节点：" + agentId);
        serverRequest.form("openStatus", true);
        serverRequest.form("protocol", protocol);
        serverRequest.form("url", url.getHost() + ":" + url.getPort());
        serverRequest.form("loginName", AgentAuthorize.getInstance().getAgentName());
        serverRequest.form("loginPwd", AgentAuthorize.getInstance().getAgentPwd());
        serverRequest.form("type", eqInstall ? "update" : "add");
        String body = serverRequest.execute().body();
        DefaultSystemLog.getLog().info("自动注册Server:" + body);
        JsonMessage regJsonMessage = JSON.parseObject(body, JsonMessage.class);
        if (regJsonMessage.getCode() == HttpStatus.HTTP_OK) {
            if (serverJson == null) {
                serverJson = new JSONObject();
            }
            if (!eqInstall) {
                serverJson.put("installId", installId);
                serverJson.put("regTime", DateTime.now().toString());
            } else {
                serverJson.put("updateTime", DateTime.now().toString());
            }
            JsonFileUtil.saveJson(file.getAbsolutePath(), serverJson);
        }
    }
}
