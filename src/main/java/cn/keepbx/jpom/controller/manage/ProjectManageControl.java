package cn.keepbx.jpom.controller.manage;

import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseController;
import cn.keepbx.jpom.common.commander.AbstractCommander;
import cn.keepbx.jpom.common.interceptor.ProjectPermission;
import cn.keepbx.jpom.model.ProjectInfoModel;
import cn.keepbx.jpom.model.UserModel;
import cn.keepbx.jpom.service.manage.ProjectInfoService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.List;

/**
 * 项目管理
 *
 * @author Administrator
 */
@Controller
@RequestMapping(value = "/manage/")
public class ProjectManageControl extends BaseController {

    @Resource
    private ProjectInfoService projectInfoService;

    /**
     * 展示项目页面
     *
     * @return page
     */
    @RequestMapping(value = "projectInfo", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String projectInfo() {
        HashSet hashSet = projectInfoService.getAllGroup();
        setAttribute("groups", hashSet);
        return "manage/projectInfo";
    }

    /**
     * 获取正在运行的项目的端口和进程id
     *
     * @param ids ids
     * @return json
     */
    @RequestMapping(value = "getProjectPort", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String getProjectPort(String ids) {
        if (StrUtil.isEmpty(ids)) {
            return JsonMessage.getString(400, "");
        }
        JSONArray jsonArray = JSONArray.parseArray(ids);
        JSONObject jsonObject = new JSONObject();
        JSONObject itemObj;
        for (Object object : jsonArray) {
            String item = object.toString();
            int pid;
            try {
                pid = AbstractCommander.getInstance().getPid(item);
            } catch (Exception e) {
                DefaultSystemLog.ERROR().error("获取端口错误", e);
                continue;
            }
            if (pid <= 0) {
                continue;
            }
            itemObj = new JSONObject();
            String port = AbstractCommander.getInstance().getMainPort(pid);
            itemObj.put("port", port);
            itemObj.put("pid", pid);
            jsonObject.put(item, itemObj);
        }
        return JsonMessage.getString(200, "", jsonObject);
    }

    /**
     * 查询所有项目
     *
     * @return json
     */
    @RequestMapping(value = "getProjectInfo", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String getProjectInfo(String group) {
        try {
            UserModel userName = getUser();
            // 查询数据
            List<ProjectInfoModel> projectInfoModels = projectInfoService.list();
            // 转换为数据
            JSONArray array = new JSONArray();
            for (ProjectInfoModel projectInfoModel : projectInfoModels) {
                if (StrUtil.isNotEmpty(group) && !group.equals(projectInfoModel.getGroup())) {
                    continue;
                }
                String id = projectInfoModel.getId();
                JSONObject object = projectInfoModel.toJson();
                object.put("manager", userName.isProject(id));
                object.put("status", projectInfoModel.isStatus(true));
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
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
            return JsonMessage.getString(500, e.getMessage());
        }
    }


    /**
     * 删除项目
     *
     * @return json
     */
    @RequestMapping(value = "deleteProject", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @ProjectPermission
    public String deleteProject() {
        ProjectInfoModel projectInfoModel = getProjectInfoModel();
        UserModel userModel = getUser();
        try {
            // 运行判断
            if (projectInfoModel.isStatus(true)) {
                return JsonMessage.getString(401, "不能删除正在运行的项目");
            }
            String userId;
            if (userModel.isSystemUser()) {
                userId = UserModel.SYSTEM_OCCUPY_NAME;
            } else {
                userId = userModel.getId();
            }
            projectInfoService.deleteProject(projectInfoModel, userId);
            return JsonMessage.getString(200, "删除成功！");
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
            return JsonMessage.getString(500, e.getMessage());
        }
    }
}
