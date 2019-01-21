package cn.jiangzeyin.controller.manage;

import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.PageUtil;
import cn.jiangzeyin.controller.BaseController;
import cn.jiangzeyin.model.ProjectInfoModel;
import cn.jiangzeyin.service.manage.CommandService;
import cn.jiangzeyin.service.manage.ManageService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Administrator
 */
@Controller
@RequestMapping(value = "/manage/")
public class ManageControl extends BaseController {

    @Resource
    private ManageService manageService;
    @Resource
    private CommandService commandService;


    /**
     * 展示项目页面
     *
     * @return page
     */
    @RequestMapping(value = "projectInfo")
    public String projectInfo() {
        return "manage/projectInfo";
    }

    /**
     * 查询所有项目
     *
     * @return json
     */
    @RequestMapping(value = "getProjectInfo")
    @ResponseBody
    public String getProjectInfo() {
        try {
            // 查询数据
            JSONObject json = manageService.getAllProjectInfo();
            // 转换为数据
            List<ProjectInfoModel> array = new ArrayList<>();
            Set<String> setKey = json.keySet();
            for (String asetKey : setKey) {
                ProjectInfoModel projectInfoModel = manageService.getProjectInfo(asetKey);
                String result = commandService.execCommand(CommandService.CommandOp.status, projectInfoModel);
                boolean status = result.contains(CommandService.RUNING_TAG);
                projectInfoModel.setStatus(status);
                array.add(projectInfoModel);
            }
            array.sort((o1, o2) -> {
                String group1 = o1.getGroup();
                String group2 = o2.getGroup();
                if (group1 == null || group2 == null) {
                    return -1;
                }
                return group1.compareTo(group2);
            });
            return PageUtil.getPaginate(200, "查询成功！", (JSONArray) JSONArray.toJSON(array));
        } catch (IOException e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
            return JsonMessage.getString(500, e.getMessage());
        }
    }

    @RequestMapping(value = "port", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String getPort(String tag) {
        // 查询数据
        try {
            ProjectInfoModel projectInfoModel = new ProjectInfoModel();
            projectInfoModel.setId(tag);
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
    @RequestMapping(value = "deleteProject", method = RequestMethod.POST)
    @ResponseBody
    public String deleteProject(String id) {
        try {
            manageService.deleteProject(id);
            return JsonMessage.getString(200, "删除成功！");
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
            return JsonMessage.getString(500, e.getMessage());
        }
    }
}
