package cn.keepbx.jpom.controller.manage;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.controller.BaseController;
import cn.keepbx.jpom.model.ProjectInfoModel;
import cn.keepbx.jpom.service.manage.ManageService;
import cn.keepbx.jpom.service.system.SystemService;
import cn.keepbx.jpom.service.user.UserService;
import com.alibaba.fastjson.JSONArray;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.io.IOException;

/**
 * 项目管理
 *
 * @author jiangzeyin
 * @date 2018/9/29
 */
@Controller
@RequestMapping(value = "/manage/")
public class EditProjectController extends BaseController {
    @Resource
    private ManageService manageService;
    @Resource
    private UserService userService;
    @Resource
    private SystemService systemService;

    @RequestMapping(value = "editProject", method = RequestMethod.GET)
    public String editProject(String id) throws IOException {
        ProjectInfoModel projectInfo = manageService.getProjectInfo(id);
        setAttribute("item", projectInfo);
        return "manage/editProject";
    }

    /**
     * 保存项目
     *
     * @param projectInfo 项目实体
     * @return json
     * @throws IOException IO
     */
    @RequestMapping(value = "saveProject", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String saveProject(ProjectInfoModel projectInfo) throws IOException {
        String id = projectInfo.getId();
        if (StrUtil.isEmptyOrUndefined(id)) {
            return JsonMessage.getString(400, "项目id不能为空");
        }
        if (Validator.isChinese(id)) {
            return JsonMessage.getString(401, "项目id不能包含中文");
        }
        String lib = projectInfo.getLib();
        String log = projectInfo.getLog();
        if (StrUtil.isEmpty(lib) || StrUtil.isEmpty(log)) {
            return JsonMessage.getString(401, "项目lib/log不能为空");
        }
        if (StrUtil.SLASH.equals(lib) || StrUtil.SLASH.equals(log)) {
            return JsonMessage.getString(401, "项目lib/log不能为顶级目录");
        }
        if (lib.contains("../") || log.contains("../")) {
            return JsonMessage.getString(401, "项目lib/log存在提升目录问题");
        }
        JSONArray jsonArray = systemService.getWhitelistDirectory();
        if (jsonArray == null) {
            return JsonMessage.getString(401, "还没有配置白名单");
        }
        int errorCount = 0;
        for (Object obj : jsonArray) {
            String wPath = obj.toString();
            if (!lib.startsWith(wPath) && !log.startsWith(wPath)) {
                errorCount++;
            }
        }
        if (errorCount == jsonArray.size()) {
            return JsonMessage.getString(401, "项目lib/log 必须在白名单目录下");
        }
        ProjectInfoModel exits = manageService.getProjectInfo(id);
        try {
            if (exits == null) {
                if (!userName.isManage()) {
                    return JsonMessage.getString(400, "管理员才能创建项目!");
                }
                //  return addProject(projectInfo);
                projectInfo.setCreateTime(DateUtil.now());
                manageService.saveProject(projectInfo);
                return JsonMessage.getString(200, "新增成功！");
            }
            boolean manager = userService.isManager(id, getUserName());
            if (!manager) {
                return JsonMessage.getString(400, "你没有对应操作权限操作!");
            }
            manageService.updateProject(projectInfo);
            return JsonMessage.getString(200, "修改成功");
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
            return JsonMessage.getString(500, e.getMessage());
        }
    }
}
