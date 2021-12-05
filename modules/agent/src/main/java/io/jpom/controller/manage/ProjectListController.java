/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 码之科技工作室
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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseAgentController;
import io.jpom.common.commander.AbstractProjectCommander;
import io.jpom.model.data.NodeProjectInfoModel;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;

/**
 * 管理的信息获取接口
 *
 * @author jiangzeyin
 * @date 2019/4/16
 */
@RestController
@RequestMapping(value = "/manage/")
public class ProjectListController extends BaseAgentController {

    /**
     * 获取项目的信息
     *
     * @param id id
     * @return item
     * @see NodeProjectInfoModel
     */
    @RequestMapping(value = "getProjectItem", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String getProjectItem(String id) {
        NodeProjectInfoModel nodeProjectInfoModel = projectInfoService.getItem(id);
        if (nodeProjectInfoModel != null) {
            // 返回实际执行的命令
            String command = AbstractProjectCommander.getInstance().buildCommand(nodeProjectInfoModel, null);
            nodeProjectInfoModel.setRunCommand(command);
        }
        return JsonMessage.getString(200, "", nodeProjectInfoModel);
    }

    /**
     * 获取所有的分组
     *
     * @return array
     */
    @RequestMapping(value = "getProjectGroup", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String getProjectGroup() {
        HashSet<String> strings = projectInfoService.getAllGroup();
        return JsonMessage.getString(200, "", strings);
    }

    /**
     * 程序项目信息
     *
     * @param group     分组
     * @param notStatus 不包含运行状态
     * @return json
     */
    @RequestMapping(value = "getProjectInfo", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String getProjectInfo(String group, String notStatus) {
        try {
            boolean status = StrUtil.isEmpty(notStatus);
            // 查询数据
            List<NodeProjectInfoModel> nodeProjectInfoModels = projectInfoService.list();
            // 转换为数据
            JSONArray array = new JSONArray();
            for (NodeProjectInfoModel nodeProjectInfoModel : nodeProjectInfoModels) {
                if (StrUtil.isNotEmpty(group) && !group.equals(nodeProjectInfoModel.getGroup())) {
                    continue;
                }
                JSONObject object = nodeProjectInfoModel.toJson();
                if (status) {
                    object.put("status", nodeProjectInfoModel.tryGetStatus());
                }
                array.add(object);
            }
            array.sort((oo1, oo2) -> {
                JSONObject o1 = (JSONObject) oo1;
                JSONObject o2 = (JSONObject) oo2;
                String group1 = o1.getString("group");
                String group2 = o2.getString("group");
                if (group1 == null || group2 == null) {
                    return -1;
                }
                return group1.compareTo(group2);
            });
            return JsonMessage.getString(200, "查询成功！", array);
        } catch (Exception e) {
            DefaultSystemLog.getLog().error(e.getMessage(), e);
            return JsonMessage.getString(500, "查询异常：" + e.getMessage());
        }
    }

    /**
     * 展示项目页面
     */
    @RequestMapping(value = "project_copy_list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String projectCopyList(String id) {
        NodeProjectInfoModel nodeProjectInfoModel = projectInfoService.getItem(id);
        if (nodeProjectInfoModel == null) {
            return JsonMessage.getString(404, "没有对应项目");
        }
        List<NodeProjectInfoModel.JavaCopyItem> javaCopyItemList = nodeProjectInfoModel.getJavaCopyItemList();
        if (CollUtil.isEmpty(javaCopyItemList)) {
            return JsonMessage.getString(404, "对应项目没有副本集");
        }
        JSONArray array = new JSONArray();
        for (NodeProjectInfoModel.JavaCopyItem javaCopyItem : javaCopyItemList) {
            JSONObject object = javaCopyItem.toJson();
            object.put("status", javaCopyItem.tryGetStatus());
            array.add(object);
        }

        return JsonMessage.getString(200, "", array);
    }
}
