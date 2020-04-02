package io.jpom.controller.manage;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseAgentController;
import io.jpom.common.commander.AbstractProjectCommander;
import io.jpom.model.data.ProjectInfoModel;
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
     * @see ProjectInfoModel
     */
    @RequestMapping(value = "getProjectItem", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getProjectItem(String id) {
        ProjectInfoModel projectInfoModel = projectInfoService.getItem(id);
        if (projectInfoModel != null) {
            // 返回实际执行的命令
            String command = AbstractProjectCommander.getInstance().buildCommand(projectInfoModel, null);
            projectInfoModel.setRunCommand(command);
        }
        return JsonMessage.getString(200, "", projectInfoModel);
    }

    /**
     * 获取所有的分组
     *
     * @return array
     */
    @RequestMapping(value = "getProjectGroup", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
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
    @RequestMapping(value = "getProjectInfo", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getProjectInfo(String group, String notStatus) {
        try {
            boolean status = StrUtil.isEmpty(notStatus);
            // 查询数据
            List<ProjectInfoModel> projectInfoModels = projectInfoService.list();
            // 转换为数据
            JSONArray array = new JSONArray();
            for (ProjectInfoModel projectInfoModel : projectInfoModels) {
                if (StrUtil.isNotEmpty(group) && !group.equals(projectInfoModel.getGroup())) {
                    continue;
                }
                JSONObject object = projectInfoModel.toJson();
                if (status) {
                    object.put("status", projectInfoModel.tryGetStatus());
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
    @RequestMapping(value = "project_copy_list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String projectCopyList(String id) {
        ProjectInfoModel projectInfoModel = projectInfoService.getItem(id);
        if (projectInfoModel == null) {
            return JsonMessage.getString(404, "没有对应项目");
        }
        List<ProjectInfoModel.JavaCopyItem> javaCopyItemList = projectInfoModel.getJavaCopyItemList();
        if (CollUtil.isEmpty(javaCopyItemList)) {
            return JsonMessage.getString(404, "对应项目没有副本集");
        }
        JSONArray array = new JSONArray();
        for (ProjectInfoModel.JavaCopyItem javaCopyItem : javaCopyItemList) {
            JSONObject object = javaCopyItem.toJson();
            object.put("status", javaCopyItem.tryGetStatus());
            array.add(object);
        }

        return JsonMessage.getString(200, "", array);
    }
}
