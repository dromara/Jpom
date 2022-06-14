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
package io.jpom.controller.manage;

import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseAgentController;
import io.jpom.common.commander.AbstractProjectCommander;
import io.jpom.model.data.NodeProjectInfoModel;
import io.jpom.service.manage.ConsoleService;
import io.jpom.socket.ConsoleCommandOp;
import io.jpom.util.CommandUtil;
import io.jpom.util.JvmUtil;
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
    public String getProjectStatus(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "项目id 不正确") String id, String getCopy) {
        NodeProjectInfoModel nodeProjectInfoModel = tryGetProjectInfoModel();
        Assert.notNull(nodeProjectInfoModel, "项目id不存在");
        JSONObject jsonObject = new JSONObject();
        try {
            CommandUtil.openCache();
            int pid = 0;
            try {
                pid = AbstractProjectCommander.getInstance().getPid(nodeProjectInfoModel, null);
            } catch (Exception e) {
                log.error("获取项目pid 失败", e);
            }
            if (pid <= 0) {
                Assert.state(JvmUtil.jpsNormal, "当前服务器 jps 命令异常,请检查 jdk 是否完整,以及 java 环境变量是否配置正确");
            }
            jsonObject.put("pId", pid);
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
        return JsonMessage.getString(200, "", jsonObject);
    }

    /**
     * 获取项目的运行端口
     *
     * @param ids ids
     * @return obj
     */
    @RequestMapping(value = "getProjectPort", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String getProjectPort(String ids) {
        Assert.hasText(ids, "没有要获取的信息");
        JSONArray jsonArray = JSONArray.parseArray(ids);
        JSONObject jsonObject = new JSONObject();
        JSONObject itemObj;
        try {
            CommandUtil.openCache();
            for (Object object : jsonArray) {
                String item = object.toString();
                int pid;
                try {
                    NodeProjectInfoModel projectInfoServiceItem = projectInfoService.getItem(item);
                    pid = AbstractProjectCommander.getInstance().getPid(projectInfoServiceItem, null);
                } catch (Exception e) {
                    log.error("获取端口错误", e);
                    continue;
                }
                if (pid <= 0) {
                    Assert.state(JvmUtil.jpsNormal, "当前服务器 jps 命令异常,请检查 jdk 是否完整,以及 java 环境变量是否配置正确");
                    continue;
                }
                itemObj = new JSONObject();
                String port = AbstractProjectCommander.getInstance().getMainPort(pid);
                itemObj.put("port", port);
                itemObj.put("pid", pid);
                jsonObject.put(item, itemObj);
            }
        } finally {
            CommandUtil.closeCache();
        }
        return JsonMessage.getString(200, "", jsonObject);
    }


    /**
     * 获取项目的运行端口
     *
     * @param id      项目id
     * @param copyIds 副本 ids ["aa","ss"]
     * @return obj
     */
    @RequestMapping(value = "getProjectCopyPort", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String getProjectPort(String id, String copyIds) {
        if (StrUtil.isEmpty(copyIds) || StrUtil.isEmpty(id)) {
            return JsonMessage.getString(400, "");
        }
        NodeProjectInfoModel nodeProjectInfoModel = getProjectInfoModel();

        JSONArray jsonArray = JSONArray.parseArray(copyIds);
        JSONObject jsonObject = new JSONObject();
        JSONObject itemObj;
        try {
            CommandUtil.openCache();
            for (Object object : jsonArray) {
                String item = object.toString();
                NodeProjectInfoModel.JavaCopyItem copyItem = nodeProjectInfoModel.findCopyItem(item);
                int pid;
                try {
                    pid = AbstractProjectCommander.getInstance().getPid(nodeProjectInfoModel, copyItem);
                    if (pid <= 0) {
                        Assert.state(JvmUtil.jpsNormal, "当前服务器 jps 命令异常,请检查 jdk 是否完整,以及 java 环境变量是否配置正确");
                        continue;
                    }
                } catch (Exception e) {
                    log.error("获取端口错误", e);
                    continue;
                }
                itemObj = new JSONObject();
                String port = AbstractProjectCommander.getInstance().getMainPort(pid);
                itemObj.put("port", port);
                itemObj.put("pid", pid);
                jsonObject.put(item, itemObj);
            }
        } finally {
            CommandUtil.closeCache();
        }
        return JsonMessage.getString(200, "", jsonObject);
    }

    @RequestMapping(value = "restart", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String restart(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "项目id 不正确") String id, String copyId) {
        NodeProjectInfoModel item = projectInfoService.getItem(id);
        Assert.notNull(item, "没有找到对应的项目");
        NodeProjectInfoModel.JavaCopyItem copyItem = item.findCopyItem(copyId);

        String result;
        try {
            result = consoleService.execCommand(ConsoleCommandOp.restart, item, copyItem);
            boolean status = AbstractProjectCommander.getInstance().isRun(item, copyItem);
            if (status) {
                return JsonMessage.getString(200, result);
            }
            return JsonMessage.getString(201, "重启项目失败：" + result);
        } catch (Exception e) {
            log.error("获取项目pid 失败", e);
            result = "error:" + e.getMessage();
            return JsonMessage.getString(500, "重启项目异常：" + result);
        }
    }


    @RequestMapping(value = "stop", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String stop(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "项目id 不正确") String id, String copyId) {
        NodeProjectInfoModel item = projectInfoService.getItem(id);
        Assert.notNull(item, "没有找到对应的项目");
        NodeProjectInfoModel.JavaCopyItem copyItem = item.findCopyItem(copyId);

        String result;
        try {
            result = consoleService.execCommand(ConsoleCommandOp.stop, item, copyItem);
            boolean status = AbstractProjectCommander.getInstance().isRun(item, copyItem);
            if (!status) {
                return JsonMessage.getString(200, result);
            }
            return JsonMessage.getString(201, "关闭项目失败：" + result);
        } catch (Exception e) {
            log.error("获取项目pid 失败", e);
            result = "error:" + e.getMessage();
            return JsonMessage.getString(500, "关闭项目异常：" + result);
        }
    }


    @RequestMapping(value = "start", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String start(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "项目id 不正确") String id, String copyId) {
        NodeProjectInfoModel item = projectInfoService.getItem(id);
        Assert.notNull(item, "没有找到对应的项目");
        NodeProjectInfoModel.JavaCopyItem copyItem = item.findCopyItem(copyId);
        String result;
        try {
            result = consoleService.execCommand(ConsoleCommandOp.start, item, copyItem);
            boolean status = AbstractProjectCommander.getInstance().isRun(item, copyItem);
            if (status) {
                return JsonMessage.getString(200, result);
            }
            return JsonMessage.getString(201, "启动项目失败：" + result);
        } catch (Exception e) {
            log.error("获取项目pid 失败", e);
            result = "error:" + e.getMessage();
            return JsonMessage.getString(500, "启动项目异常：" + result);
        }
    }
}
