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
package io.jpom.controller.script;

import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson2.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.common.JsonMessage;
import io.jpom.common.validator.ValidatorItem;
import io.jpom.common.validator.ValidatorRule;
import io.jpom.model.script.ScriptExecuteLogModel;
import io.jpom.model.script.ScriptModel;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.service.script.ScriptExecuteLogServer;
import io.jpom.service.script.ScriptServer;
import io.jpom.socket.ServerScriptProcessBuilder;
import io.jpom.util.CommandUtil;
import io.jpom.util.FileUtils;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import top.jpom.model.PageResultDto;

import java.io.File;

/**
 * @author bwcx_jzy
 * @since 2022/1/20
 */

@RestController
@RequestMapping(value = "/script_log")
@Feature(cls = ClassFeature.SCRIPT_LOG)
public class ScriptLogController extends BaseServerController {

    private final ScriptExecuteLogServer scriptExecuteLogServer;
    private final ScriptServer scriptServer;

    public ScriptLogController(ScriptExecuteLogServer scriptExecuteLogServer,
                               ScriptServer scriptServer) {
        this.scriptExecuteLogServer = scriptExecuteLogServer;
        this.scriptServer = scriptServer;
    }

    /**
     * get script log list
     *
     * @return json
     */
    @RequestMapping(value = "list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public JsonMessage<PageResultDto<ScriptExecuteLogModel>> scriptList() {
        PageResultDto<ScriptExecuteLogModel> pageResultDto = scriptExecuteLogServer.listPage(getRequest());
        return JsonMessage.success("success", pageResultDto);
    }

    /**
     * 删除日志
     *
     * @param id        id
     * @param executeId 执行ID
     * @return json
     */
    @RequestMapping(value = "del_log", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public JsonMessage<Object> delLog(@ValidatorItem() String id,
                                      @ValidatorItem() String executeId) {
        ScriptModel item = scriptServer.getByKey(id, getRequest());
        Assert.notNull(item, "没有对应数据");
        File logFile = item.logFile(executeId);
        boolean fastDel = CommandUtil.systemFastDel(logFile);
        Assert.state(!fastDel, "删除日志文件失败");
        scriptExecuteLogServer.delByKey(executeId);
        return JsonMessage.success("删除成功");
    }

    /**
     * 获取的日志
     *
     * @param id        id
     * @param executeId 执行ID
     * @param line      需要获取的行号
     * @return json
     */
    @RequestMapping(value = "log", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public JsonMessage<JSONObject> getNowLog(@ValidatorItem() String id,
                                             @ValidatorItem() String executeId,
                                             @ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "line") int line) {
        ScriptModel item = scriptServer.getByKey(id, getRequest());
        Assert.notNull(item, "没有对应数据");
        File logFile = item.logFile(executeId);
        Assert.state(FileUtil.isFile(logFile), "日志文件错误");
        JSONObject data = FileUtils.readLogFile(logFile, line);
        // 运行中
        data.put("run", ServerScriptProcessBuilder.isRun(executeId));
        return JsonMessage.success("ok", data);
    }
}
