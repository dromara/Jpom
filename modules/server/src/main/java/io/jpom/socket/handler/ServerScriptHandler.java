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
package io.jpom.socket.handler;

import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.spring.SpringUtil;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.model.data.UserModel;
import io.jpom.model.script.ScriptExecuteLogModel;
import io.jpom.model.script.ScriptModel;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.service.script.ScriptExecuteLogServer;
import io.jpom.service.script.ScriptServer;
import io.jpom.socket.BaseProxyHandler;
import io.jpom.socket.ConsoleCommandOp;
import io.jpom.socket.ScriptProcessBuilder;
import io.jpom.util.SocketSessionUtil;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;

/**
 * 服务端脚本日志
 *
 * @author bwcx_jzy
 * @since 2022/1/19
 */
@Feature(cls = ClassFeature.SCRIPT, method = MethodFeature.EXECUTE)
public class ServerScriptHandler extends BaseProxyHandler {

    private ScriptExecuteLogServer logServer;
    private ScriptServer nodeScriptServer;

    @Override
    protected void init(WebSocketSession session, Map<String, Object> attributes) throws Exception {
        super.init(session, attributes);
        //
        this.logServer = SpringUtil.getBean(ScriptExecuteLogServer.class);
        this.nodeScriptServer = SpringUtil.getBean(ScriptServer.class);
        ScriptModel scriptModel = (ScriptModel) attributes.get("dataItem");
        this.sendMsg(session, "连接成功：" + scriptModel.getName());
    }

    public ServerScriptHandler() {
        super(null);
    }

    @Override
    protected Object[] getParameters(Map<String, Object> attributes) {
        return new Object[0];
    }

    @Override
    protected String handleTextMessage(Map<String, Object> attributes, WebSocketSession session, JSONObject json, ConsoleCommandOp consoleCommandOp) throws IOException {
        ScriptModel scriptModel = (ScriptModel) attributes.get("dataItem");
        if (consoleCommandOp != ConsoleCommandOp.heart) {
            super.logOpt(this.getClass(), attributes, json);
            switch (consoleCommandOp) {
                case start: {

                    String args = json.getString("args");
                    String executeId = this.createLog(attributes, scriptModel);
                    json.put("executeId", executeId);
                    ScriptProcessBuilder.addWatcher(scriptModel, executeId, args, session);
                    this.sendMsg(session, json.toString());
                    break;
                }
                case stop: {
                    String executeId = json.getString("executeId");
                    if (StrUtil.isEmpty(executeId)) {
                        SocketSessionUtil.send(session, "没有执行ID");
                        session.close();
                        return null;
                    }
                    ScriptProcessBuilder.stopRun(executeId);
                    break;
                }
                default:
                    return null;
            }
        }
        return null;
    }

    /**
     * 创建执行日志
     *
     * @param attributes 参数属性
     * @return 执行ID
     */
    private String createLog(Map<String, Object> attributes, ScriptModel scriptModel) {
        UserModel userModel = (UserModel) attributes.get("userInfo");

        //
        try {
            BaseServerController.resetInfo(userModel);
            //
            ScriptModel scriptCacheModel = new ScriptModel();
            scriptCacheModel.setId(scriptModel.getId());
            scriptCacheModel.setLastRunUser(userModel.getId());
            nodeScriptServer.update(scriptCacheModel);
            //
            ScriptExecuteLogModel scriptExecuteLogCacheModel = logServer.create(scriptModel, 0);
            return scriptExecuteLogCacheModel.getId();
        } finally {
            BaseServerController.removeAll();
        }
    }


    @Override
    public void destroy(WebSocketSession session) {
        //
        super.destroy(session);
        ScriptProcessBuilder.stopWatcher(session);
    }
}
