/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.controller.manage;

import cn.hutool.core.lang.Tuple;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.BaseAgentController;
import org.dromara.jpom.common.commander.ProjectCommander;
import org.dromara.jpom.model.RunMode;
import org.dromara.jpom.model.data.DslYmlDto;
import org.dromara.jpom.model.data.NodeProjectInfoModel;
import org.dromara.jpom.service.script.DslScriptServer;
import org.dromara.jpom.socket.ConsoleCommandOp;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理的信息获取接口
 *
 * @author bwcx_jzy
 * @since 2019/4/16
 */
@RestController
@RequestMapping(value = "/manage/")
@Slf4j
public class ProjectListController extends BaseAgentController {

    private final ProjectCommander projectCommander;
    private final DslScriptServer dslScriptServer;

    public ProjectListController(ProjectCommander projectCommander,
                                 DslScriptServer dslScriptServer) {
        this.projectCommander = projectCommander;
        this.dslScriptServer = dslScriptServer;
    }

    /**
     * 获取项目的信息
     *
     * @param id id
     * @return item
     * @see NodeProjectInfoModel
     */
    @RequestMapping(value = "getProjectItem", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<NodeProjectInfoModel> getProjectItem(String id) {
        NodeProjectInfoModel nodeProjectInfoModel = projectInfoService.getItem(id);
        if (nodeProjectInfoModel != null) {
            RunMode runMode = nodeProjectInfoModel.getRunMode();
            if (runMode != RunMode.Dsl && runMode != RunMode.File) {
                // 返回实际执行的命令
                String command = projectCommander.buildRunCommand(nodeProjectInfoModel);
                nodeProjectInfoModel.setRunCommand(command);
            }
            if (runMode == RunMode.Dsl) {
                DslYmlDto dslYmlDto = nodeProjectInfoModel.mustDslConfig();
                boolean reload = dslYmlDto.hasRunProcess(ConsoleCommandOp.reload.name());
                nodeProjectInfoModel.setCanReload(reload);
                // 查询 dsl 流程信息
                List<JSONObject> list = Arrays.stream(ConsoleCommandOp.values())
                    .filter(ConsoleCommandOp::isCanOpt)
                    .map(consoleCommandOp -> {
                        Tuple tuple = dslScriptServer.resolveProcessScript(nodeProjectInfoModel, dslYmlDto, consoleCommandOp);
                        JSONObject jsonObject = tuple.get(0);
                        jsonObject.put("process", consoleCommandOp);
                        return jsonObject;
                    })
                    .collect(Collectors.toList());
                nodeProjectInfoModel.setDslProcessInfo(list);
            }
        }
        return JsonMessage.success("", nodeProjectInfoModel);
    }

    /**
     * 程序项目信息
     *
     * @return json
     */
    @RequestMapping(value = "getProjectInfo", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<List<NodeProjectInfoModel>> getProjectInfo() {
        // 查询数据
        List<NodeProjectInfoModel> nodeProjectInfoModels = projectInfoService.list();
        return JsonMessage.success("", nodeProjectInfoModels);
    }
}
