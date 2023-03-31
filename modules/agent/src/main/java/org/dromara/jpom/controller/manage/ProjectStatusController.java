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

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.common.BaseAgentController;
import org.dromara.jpom.common.JsonMessage;
import org.dromara.jpom.common.commander.AbstractProjectCommander;
import org.dromara.jpom.common.commander.CommandOpResult;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.common.validator.ValidatorRule;
import org.dromara.jpom.model.data.NodeProjectInfoModel;
import org.dromara.jpom.service.manage.ConsoleService;
import org.dromara.jpom.socket.ConsoleCommandOp;
import org.dromara.jpom.util.CommandUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 项目文件管理
 *
 * @author jiangzeyin
 * @since 2019/4/17
 */
@RestController
@RequestMapping(value = "/manage/")
@Slf4j
public class ProjectStatusController extends BaseAgentController {

    private final ConsoleService consoleService;

    public ProjectStatusController(ConsoleService consoleService) {
        this.consoleService = consoleService;
    }

    /**
     * 获取项目的进程id
     *
     * @param id 项目id
     * @return json
     */
    @RequestMapping(value = "getProjectStatus", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonMessage<JSONObject> getProjectStatus(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "项目id 不正确") String id, String getCopy) {
        NodeProjectInfoModel nodeProjectInfoModel = tryGetProjectInfoModel();
        Assert.notNull(nodeProjectInfoModel, "项目id不存在");
        JSONObject jsonObject = new JSONObject();
        try {
            CommandUtil.openCache();
            try {
                CommandOpResult status = AbstractProjectCommander.getInstance().status(nodeProjectInfoModel, null);
                jsonObject.put("pId", status.getPid());
                jsonObject.put("pIds", status.getPids());
                jsonObject.put("statusMsg", status.getStatusMsg());
            } catch (Exception e) {
                log.error("获取项目pid 失败", e);
            }
            //
            if (StrUtil.isNotEmpty(getCopy)) {
                List<NodeProjectInfoModel.JavaCopyItem> javaCopyItemList = nodeProjectInfoModel.getJavaCopyItemList();
                JSONArray copys = new JSONArray();
                if (javaCopyItemList != null) {
                    for (NodeProjectInfoModel.JavaCopyItem javaCopyItem : javaCopyItemList) {
                        JSONObject jsonObject1 = new JSONObject();
                        jsonObject1.put("copyId", javaCopyItem.getId());
                        boolean run = AbstractProjectCommander.getInstance().isRun(nodeProjectInfoModel, javaCopyItem);
                        jsonObject1.put("status", run);
                        copys.add(jsonObject1);
                    }
                }
                jsonObject.put("copys", copys);
            }
        } finally {
            CommandUtil.closeCache();
        }
        return JsonMessage.success("", jsonObject);
    }

    /**
     * 获取项目的运行端口
     *
     * @param ids ids
     * @return obj
     */
    @RequestMapping(value = "getProjectPort", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonMessage<JSONObject> getProjectPort(String ids) {
        Assert.hasText(ids, "没有要获取的信息");
        JSONArray jsonArray = JSONArray.parseArray(ids);
        JSONObject jsonObject = new JSONObject();
        try {
            CommandUtil.openCache();
            for (Object object : jsonArray) {
                String item = object.toString();
                JSONObject itemObj = new JSONObject();
                try {
                    NodeProjectInfoModel projectInfoServiceItem = projectInfoService.getItem(item);
                    itemObj.put("name", projectInfoServiceItem.getName());
                    CommandOpResult commandOpResult = AbstractProjectCommander.getInstance().status(projectInfoServiceItem, null);
                    int pid = commandOpResult.getPid();
                    //
                    itemObj.put("pid", pid);
                    itemObj.put("pids", commandOpResult.getPids());
                    itemObj.put("statusMsg", commandOpResult.getStatusMsg());
                    if (StrUtil.isNotEmpty(commandOpResult.getPorts())) {
                        itemObj.put("port", commandOpResult.getPorts());
                    } else {
                        String port = AbstractProjectCommander.getInstance().getMainPort(pid);
                        itemObj.put("port", port);
                    }
                } catch (Exception e) {
                    log.error("获取端口错误", e);
                    itemObj.put("error", e.getMessage());
                }
                jsonObject.put(item, itemObj);
            }
        } finally {
            CommandUtil.closeCache();
        }
        return JsonMessage.success("", jsonObject);
    }


    /**
     * 获取项目的运行端口
     *
     * @param id      项目id
     * @param copyIds 副本 ids ["aa","ss"]
     * @return obj
     */
    @RequestMapping(value = "getProjectCopyPort", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonMessage<JSONObject> getProjectPort(String id, String copyIds) {
        if (StrUtil.isEmpty(copyIds) || StrUtil.isEmpty(id)) {
            return new JsonMessage<>(400, "参数异常");
        }
        NodeProjectInfoModel nodeProjectInfoModel = getProjectInfoModel();

        JSONArray jsonArray = JSONArray.parseArray(copyIds);
        JSONObject jsonObject = new JSONObject();

        try {
            CommandUtil.openCache();
            for (Object object : jsonArray) {
                String item = object.toString();
                NodeProjectInfoModel.JavaCopyItem copyItem = nodeProjectInfoModel.findCopyItem(item);
                JSONObject itemObj = new JSONObject();
                if (copyItem != null) {
                    try {
                        CommandOpResult status = AbstractProjectCommander.getInstance().status(nodeProjectInfoModel, copyItem);
                        int pid = status.getPid();
                        String port = AbstractProjectCommander.getInstance().getMainPort(pid);
                        itemObj.put("port", port);
                        itemObj.put("pid", pid);
                        itemObj.put("pids", status.getPids());
                        itemObj.put("statusMsg", status.getStatusMsg());
                    } catch (Exception e) {
                        log.error("获取端口错误", e);
                        itemObj.put("error", e.getMessage());
                    }
                } else {
                    itemObj.put("error", "对应的副本不存在");
                }
                jsonObject.put(item, itemObj);
            }
        } finally {
            CommandUtil.closeCache();
        }
        return JsonMessage.success("", jsonObject);
    }

    @RequestMapping(value = "restart", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonMessage<CommandOpResult> restart(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "项目id 不正确") String id, String copyId) {
        NodeProjectInfoModel item = projectInfoService.getItem(id);
        Assert.notNull(item, "没有找到对应的项目");
        NodeProjectInfoModel.JavaCopyItem copyItem = item.findCopyItem(copyId);
        try {
            CommandOpResult result = consoleService.execCommand(ConsoleCommandOp.restart, item, copyItem);
            // boolean status = AbstractProjectCommander.getInstance().isRun(item, copyItem);

            return new JsonMessage<>(result.isSuccess() ? 200 : 201, result.isSuccess() ? "操作成功" : "操作失败:" + result.msgStr(), result);

        } catch (Exception e) {
            log.error("重启项目异常", e);
            return new JsonMessage<>(500, "重启项目异常:" + e.getMessage());
        }
    }


    @RequestMapping(value = "stop", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonMessage<CommandOpResult> stop(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "项目id 不正确") String id, String copyId) {
        NodeProjectInfoModel item = projectInfoService.getItem(id);
        Assert.notNull(item, "没有找到对应的项目");
        NodeProjectInfoModel.JavaCopyItem copyItem = item.findCopyItem(copyId);

        try {
            CommandOpResult result = consoleService.execCommand(ConsoleCommandOp.stop, item, copyItem);
            return new JsonMessage<>(result.isSuccess() ? 200 : 201, result.isSuccess() ? "操作成功" : "操作失败:" + result.msgStr(), result);
        } catch (Exception e) {
            log.error("关闭项目异常", e);
            return new JsonMessage<>(500, "关闭项目异常：" + e.getMessage());
        }
    }


    @RequestMapping(value = "start", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonMessage<CommandOpResult> start(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "项目id 不正确") String id, String copyId) {
        NodeProjectInfoModel item = projectInfoService.getItem(id);
        Assert.notNull(item, "没有找到对应的项目");
        NodeProjectInfoModel.JavaCopyItem copyItem = item.findCopyItem(copyId);

        try {
            CommandOpResult result = consoleService.execCommand(ConsoleCommandOp.start, item, copyItem);
            return new JsonMessage<>(result.isSuccess() ? 200 : 201, result.isSuccess() ? "操作成功" : "操作失败:" + result.msgStr(), result);
        } catch (Exception e) {
            log.error("获取项目pid 失败", e);
            return new JsonMessage<>(500, "启动项目异常：" + e.getMessage());
        }
    }
}
