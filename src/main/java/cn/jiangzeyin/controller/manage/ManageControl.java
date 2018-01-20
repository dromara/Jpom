package cn.jiangzeyin.controller.manage;

import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.base.AbstractBaseControl;
import cn.jiangzeyin.common.interceptor.LoginInterceptor;
import cn.jiangzeyin.model.ProjectInfoModel;
import cn.jiangzeyin.service.manage.ManageService;
import cn.jiangzeyin.system.log.SystemLog;
import cn.jiangzeyin.util.StringUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping(value = "/manage/")
public class ManageControl extends AbstractBaseControl {

    @Resource
    ManageService manageService;


    /**
     * 展示项目页面
     * @return
     */
    @RequestMapping(value = "projectInfo")
    public String projectInfo() {

        return "manage/projectInfo";
    }

    /**
     * 管理项目
     * @return
     */
    @RequestMapping(value = "console")
    public String console(String id) {
        ProjectInfoModel pim = null;
        try {
            pim = manageService.getProjectInfo(id);
        } catch (IOException e) {
            e.printStackTrace();
            SystemLog.LOG().error(e.getMessage(), e);
        }
        request.setAttribute("projectInfo", JSONObject.toJSONString(pim));
        request.setAttribute("userInfo", StringUtil.getMd5(String.format("%s:%s", session.getAttribute(LoginInterceptor.SESSION_NAME), session.getAttribute(LoginInterceptor.SESSION_PWD))));
        return "manage/console";
    }



    /**
     * 查询所有项目
     * @return
     */
    @RequestMapping(value = "getProjectInfo")
    @ResponseBody
    public String getProjectInfor() {
        try {
            // 查询数据
            JSONObject json = manageService.getAllProjectInfo();

            // 转换为数据
            JSONArray array = new JSONArray();

            Set<String> set_key = json.keySet();
            Iterator<String> iterator = set_key.iterator();

            while (iterator.hasNext()) {
                array.add(json.get(iterator.next()));
            }

            return JsonMessage.getPaginate(200, "查询成功！", array);
        } catch (IOException e) {
            e.printStackTrace();
            SystemLog.LOG().error(e.getMessage(), e);
            return JsonMessage.getString(500, e.getMessage());
        }
    }


    /**
     * 添加项目
     * @return
     */
    @RequestMapping(value = "addProject", method = RequestMethod.POST)
    @ResponseBody
    public String addProject(ProjectInfoModel projectInfo) {

        projectInfo.setCreateTime(StringUtil.currTime());

        try {
            manageService.saveProject(projectInfo);

            return JsonMessage.getString(200, "新增成功！");
        } catch (Exception e) {
            e.printStackTrace();
            SystemLog.LOG().error(e.getMessage(), e);
            return JsonMessage.getString(500, e.getMessage());
        }
    }

    /**
     * 删除项目
     * @param id
     * @return
     */
    @RequestMapping(value = "deleteProject", method = RequestMethod.POST)
    @ResponseBody
    public String deleteProject(String id) {

        try {
            manageService.deleteProject(id);

            return JsonMessage.getString(200, "删除成功！");
        } catch (Exception e) {
            e.printStackTrace();
            SystemLog.LOG().error(e.getMessage(), e);
            return JsonMessage.getString(500, e.getMessage());
        }
    }


    /**
     * 配置项目信息
     * @param projectInfo
     * @return
     */
    @RequestMapping(value = "updateProject", method = RequestMethod.POST)
    @ResponseBody
    public String updateProject(ProjectInfoModel projectInfo) {

        projectInfo.setCreateTime(StringUtil.currTime());

        try {
            manageService.updateProject(projectInfo);

            return JsonMessage.getString(200, "配置成功！");
        } catch (Exception e) {
            e.printStackTrace();
            SystemLog.LOG().error(e.getMessage(), e);
            return JsonMessage.getString(500, e.getMessage());
        }
    }
}
