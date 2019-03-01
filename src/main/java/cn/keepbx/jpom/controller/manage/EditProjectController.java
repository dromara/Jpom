package cn.keepbx.jpom.controller.manage;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
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
import java.io.File;
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

    @RequestMapping(value = "editProject", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String editProject(String id) throws IOException {
        ProjectInfoModel projectInfo = manageService.getProjectInfo(id);


        // 白名单
        JSONArray jsonArray = systemService.getWhitelistDirectory();
        setAttribute("whitelistDirectory", jsonArray);
        for (Object obj : jsonArray) {
            String path = obj.toString();
            String lib = projectInfo.getLib();
            if (lib.startsWith(path)) {
                lib = lib.substring(path.length());
                projectInfo.setLib(lib);
                break;
            }
        }
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
    public String saveProject(ProjectInfoModel projectInfo, String whitelistDirectory) throws IOException {
        String id = projectInfo.getId();
        if (StrUtil.isEmptyOrUndefined(id)) {
            return JsonMessage.getString(400, "项目id不能为空");
        }
        if (Validator.isChinese(id)) {
            return JsonMessage.getString(401, "项目id不能包含中文");
        }
        if (StrUtil.isEmpty(whitelistDirectory)) {
            return JsonMessage.getString(401, "项目路径不能为空");
        }
        JSONArray jsonArray = systemService.getWhitelistDirectory();
        if (jsonArray == null) {
            return JsonMessage.getString(401, "还没有配置白名单");
        }
        if (!jsonArray.contains(whitelistDirectory)) {
            return JsonMessage.getString(401, "请选择正确的项目路径");
        }
        String lib = projectInfo.getLib();
//        String log = projectInfo.getLog();
        if (StrUtil.isEmpty(lib)) {
            return JsonMessage.getString(401, "项目lib不能为空");
        }
        if (StrUtil.SLASH.equals(lib)) {
            return JsonMessage.getString(401, "项目lib不能为顶级目录");
        }
        if (lib.contains("../")) {
            return JsonMessage.getString(401, "项目lib存在提升目录问题");
        }
        lib = String.format("%s/%s", whitelistDirectory, lib);
        projectInfo.setLib(FileUtil.normalize(lib));

        String log = new File(lib).getParent();
        log = String.format("%s/run.log", log);
        projectInfo.setLog(FileUtil.normalize(log));

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
//            boolean manager = userService.isManager(id, getUserName());
            if (!userName.isProject(id)) {
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
