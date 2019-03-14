package cn.keepbx.jpom.controller.manage;

import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseController;
import cn.keepbx.jpom.common.PageUtil;
import cn.keepbx.jpom.model.ProjectInfoModel;
import cn.keepbx.jpom.model.UserModel;
import cn.keepbx.jpom.service.manage.CommandService;
import cn.keepbx.jpom.service.manage.ProjectInfoService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.io.IOException;
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
    @Resource
    private CommandService commandService;

    /**
     * 展示项目页面
     *
     * @return page
     */
    @RequestMapping(value = "projectInfo", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String projectInfo() throws IOException {
        //获取所有分组
        List<ProjectInfoModel> projectInfoModels = projectInfoService.list();
        HashSet<String> hashSet = new HashSet<>();
        for (ProjectInfoModel projectInfoModel : projectInfoModels) {
            hashSet.add(projectInfoModel.getGroup());
        }
        setAttribute("groups", hashSet);
        return "manage/projectInfo";
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
                String result = commandService.execCommand(CommandService.CommandOp.status, projectInfoModel);
                boolean status = result.contains(CommandService.RUNING_TAG);
                projectInfoModel.setStatus(status);
                String id = projectInfoModel.getId();
                JSONObject object = (JSONObject) JSONObject.toJSON(projectInfoModel);
                object.put("manager", userName.isProject(id));
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
            return PageUtil.getPaginate(200, "查询成功！", array);
        } catch (IOException e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
            return JsonMessage.getString(500, e.getMessage());
        }
    }

    @RequestMapping(value = "port", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String getPort(String tag) {
        // 查询数据
        try {
            ProjectInfoModel projectInfoModel = projectInfoService.getItem(tag);
            String pId = commandService.execCommand(CommandService.CommandOp.pid, projectInfoModel, null).trim();
            if (StrUtil.isNotEmpty(pId)) {
                String result = commandService.execSystemCommand("netstat -antup | grep " + pId);
                setAttribute("port", result);
                return "manage/port";
            }
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
        }
        return "manage/port";
    }


    /**
     * 删除项目
     *
     * @param id id
     * @return json
     */
    @RequestMapping(value = "deleteProject", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String deleteProject(String id, String type) throws IOException {
        UserModel userName = getUser();
        if (!userName.isProject(id)) {
            return JsonMessage.getString(500, "你没有对应权限");
        }
        ProjectInfoModel projectInfoModel = projectInfoService.getItem(id);
        if (projectInfoModel == null) {
            return JsonMessage.getString(405, "未找到对应项目");
        }
        //
        //        if ("all".equals(type)) {
        //            if (ConfigBean.getInstance().safeMode) {
        //                return JsonMessage.getString(405, "安全模式下，不允许彻底删除项目");
        //            }
        //            if (!UserModel.SYSTEM_ADMIN.equals(userName.getParent())) {
        //                return JsonMessage.getString(405, "只有系统管理员才能彻底删除项目");
        //            }
        //            //
        //            commandService.execCommand(CommandService.CommandOp.stop, projectInfoModel);
        //            //
        //            if (!FileUtil.del(projectInfoModel.getLib())) {
        //                return JsonMessage.getString(500, "清除项目lib失败");
        //            }
        //            if (!FileUtil.del(projectInfoModel.getLogBack())) {
        //                return JsonMessage.getString(500, "清除项目日志备份失败");
        //            }
        //            //
        //            if (!FileUtil.del(projectInfoModel.getLog())) {
        //                return JsonMessage.getString(500, "清除项目控制台日志文件失败");
        //            }
        //        }
        try {
            projectInfoService.deleteProject(id);
            return JsonMessage.getString(200, "删除成功！");
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
            return JsonMessage.getString(500, e.getMessage());
        }
    }
}
