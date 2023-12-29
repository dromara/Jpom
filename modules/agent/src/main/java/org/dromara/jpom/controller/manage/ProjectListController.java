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
package org.dromara.jpom.controller.manage;

import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.BaseAgentController;
import org.dromara.jpom.common.commander.ProjectCommander;
import org.dromara.jpom.model.RunMode;
import org.dromara.jpom.model.data.DslYmlDto;
import org.dromara.jpom.model.data.NodeProjectInfoModel;
import org.dromara.jpom.socket.ConsoleCommandOp;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    public ProjectListController(ProjectCommander projectCommander) {
        this.projectCommander = projectCommander;
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
                String command = projectCommander.buildJavaCommand(nodeProjectInfoModel);
                nodeProjectInfoModel.setRunCommand(command);
            }
            if (runMode == RunMode.Dsl) {
                DslYmlDto dslYmlDto = nodeProjectInfoModel.dslConfig();
                boolean reload = dslYmlDto.hasRunProcess(ConsoleCommandOp.reload.name());
                nodeProjectInfoModel.setCanReload(reload);
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
