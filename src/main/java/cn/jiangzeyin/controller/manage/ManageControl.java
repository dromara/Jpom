package cn.jiangzeyin.controller.manage;

import cn.jiangzeyin.DateUtil;
import cn.jiangzeyin.Md5Util;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.PageUtil;
import cn.jiangzeyin.common.interceptor.LoginInterceptor;
import cn.jiangzeyin.controller.base.AbstractBaseControl;
import cn.jiangzeyin.model.ProjectInfoModel;
import cn.jiangzeyin.service.manage.ManageService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.Set;

@Controller
@RequestMapping(value = "/manage/")
public class ManageControl extends AbstractBaseControl {

    @Resource
    private ManageService manageService;


    /**
     * 展示项目页面
     *
     * @return
     */
    @RequestMapping(value = "projectInfo")
    public String projectInfo() {

        return "manage/projectInfo";
    }

    /**
     * 管理项目
     *
     * @return
     */
    @RequestMapping(value = "console")
    public String console(String id) throws NoSuchAlgorithmException {
        ProjectInfoModel pim = null;
        try {
            pim = manageService.getProjectInfo(id);
        } catch (IOException e) {
            e.printStackTrace();
            DefaultSystemLog.LOG().error(e.getMessage(), e);
        }
        setAttribute("projectInfo", JSONObject.toJSONString(pim));
        setAttribute("userInfo", Md5Util.getString(String.format("%s:%s", getSession().getAttribute(LoginInterceptor.SESSION_NAME), getSession().getAttribute(LoginInterceptor.SESSION_PWD))));
        return "manage/console";
    }


    /**
     * 查询所有项目
     *
     * @return
     */
    @RequestMapping(value = "getProjectInfo")
    @ResponseBody
    public String getProjectInfo() {
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

            return PageUtil.getPaginate(200, "查询成功！", array);
        } catch (IOException e) {
            e.printStackTrace();
            DefaultSystemLog.LOG().error(e.getMessage(), e);
            return JsonMessage.getString(500, e.getMessage());
        }
    }


    /**
     * 添加项目
     *
     * @return
     */
    @RequestMapping(value = "addProject", method = RequestMethod.POST)
    @ResponseBody
    public String addProject(ProjectInfoModel projectInfo) {

        projectInfo.setCreateTime(DateUtil.getCurrentFormatTime(null));

        try {
            manageService.saveProject(projectInfo);

            return JsonMessage.getString(200, "新增成功！");
        } catch (Exception e) {
            e.printStackTrace();
            DefaultSystemLog.LOG().error(e.getMessage(), e);
            return JsonMessage.getString(500, e.getMessage());
        }
    }

    /**
     * 删除项目
     *
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
            DefaultSystemLog.LOG().error(e.getMessage(), e);
            return JsonMessage.getString(500, e.getMessage());
        }
    }


    /**
     * 配置项目信息
     *
     * @param projectInfo
     * @return
     */
    @RequestMapping(value = "updateProject", method = RequestMethod.POST)
    @ResponseBody
    public String updateProject(ProjectInfoModel projectInfo) {

        projectInfo.setCreateTime(DateUtil.getCurrentFormatTime(null));

        try {
            manageService.updateProject(projectInfo);

            return JsonMessage.getString(200, "配置成功！");
        } catch (Exception e) {
            e.printStackTrace();
            DefaultSystemLog.LOG().error(e.getMessage(), e);
            return JsonMessage.getString(500, e.getMessage());
        }
    }
}
