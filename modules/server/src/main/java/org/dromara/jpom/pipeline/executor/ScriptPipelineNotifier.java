/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.pipeline.executor;

import cn.hutool.core.util.StrUtil;
import org.dromara.jpom.func.assets.model.ScriptLibraryModel;
import org.dromara.jpom.func.assets.server.ScriptLibraryServer;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.io.File;
import java.util.function.Consumer;

/**
 * 通知实现：把「脚本库里的一条脚本」当通知渠道
 * <p>
 * 复用已有脚本库（企业微信/邮件/自定义 webhook 都由脚本自己实现），流水线只负责在正确时机带变量调用；
 * 这样通知渠道不用在流水线里再造一套。
 *
 * @author jpom
 * @since 2.11.16
 */
@Service
public class ScriptPipelineNotifier implements PipelineNotifier {

    private final ScriptLibraryServer scriptLibraryServer;
    private final PipelineCommandRunner commandRunner = new DefaultPipelineCommandRunner();

    public ScriptPipelineNotifier(ScriptLibraryServer scriptLibraryServer) {
        this.scriptLibraryServer = scriptLibraryServer;
    }

    @Override
    public String notify(String target, String event, Map<String, String> payload) throws Exception {
        if (StrUtil.isEmpty(target)) {
            return "empty notify target";
        }
        ScriptLibraryModel script = scriptLibraryServer.getByKey(target, false);
        if (script == null) {
            return "notify script not found: " + target;
        }
        Map<String, String> env = new LinkedHashMap<>(payload);
        env.put("PIPELINE_EVENT", StrUtil.nullToEmpty(event));
        Consumer<String> sink = line -> {
            // 通知脚本输出只进系统日志，不污染阶段日志
        };
        int code = commandRunner.run(new File("."), env, script.getScript(), sink, process -> {
        });
        return code == 0 ? null : "notify script exit " + code;
    }
}