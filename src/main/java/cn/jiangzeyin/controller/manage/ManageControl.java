package cn.jiangzeyin.controller.manage;

import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.PageUtil;
import cn.jiangzeyin.controller.base.AbstractBaseControl;
import cn.jiangzeyin.model.ProjectInfoModel;
import cn.jiangzeyin.service.manage.CommandService;
import cn.jiangzeyin.service.manage.ManageService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Set;

@Controller
@RequestMapping(value = "/manage/")
public class ManageControl extends AbstractBaseControl {

    @Resource
    private ManageService manageService;
    @Resource
    private CommandService commandService;


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
            Set<String> setKey = json.keySet();
            for (String asetKey : setKey) {
                ProjectInfoModel projectInfoModel = manageService.getProjectInfo(asetKey);
                String result = commandService.execCommand(CommandService.CommandOp.status, projectInfoModel, null);
                JSONObject jsonObject = json.getJSONObject(asetKey);
                boolean status = result.contains(CommandService.RUNING_TAG);
                jsonObject.put("status", status);
                array.add(jsonObject);
            }
            return PageUtil.getPaginate(200, "查询成功！", array);
        } catch (IOException e) {
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
            DefaultSystemLog.LOG().error(e.getMessage(), e);
            return JsonMessage.getString(500, e.getMessage());
        }
    }
}
