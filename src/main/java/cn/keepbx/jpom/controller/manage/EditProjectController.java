package cn.keepbx.jpom.controller.manage;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.PatternPool;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseController;
import cn.keepbx.jpom.model.ProjectInfoModel;
import cn.keepbx.jpom.model.UserModel;
import cn.keepbx.jpom.service.manage.ProjectInfoService;
import cn.keepbx.jpom.service.system.WhitelistDirectoryService;
import cn.keepbx.jpom.socket.LogWebSocketHandle;
import com.alibaba.fastjson.JSONArray;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.List;

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
    private ProjectInfoService projectInfoService;
    @Resource
    private WhitelistDirectoryService whitelistDirectoryService;

    /**
     * 修改项目页面
     *
     * @param id 项目Id
     * @return json
     * @throws IOException IO
     */
    @RequestMapping(value = "editProject", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String editProject(String id) throws IOException {
        ProjectInfoModel projectInfo = projectInfoService.getItem(id);

        // 白名单
        JSONArray jsonArray = whitelistDirectoryService.getProjectDirectory();
        setAttribute("whitelistDirectory", jsonArray);

        if (projectInfo != null && jsonArray != null) {
            for (Object obj : jsonArray) {
                String path = obj.toString();
                String lib = projectInfo.getLib();
                if (lib.startsWith(path)) {
                    String itemWhitelistDirectory = lib.substring(0, path.length());
                    lib = lib.substring(path.length());
                    setAttribute("itemWhitelistDirectory", itemWhitelistDirectory);
                    projectInfo.setLib(lib);
                    break;
                }
            }
        }
        setAttribute("item", projectInfo);
        // 运行模式
        ProjectInfoModel.RunMode[] runModes = ProjectInfoModel.RunMode.values();
        setAttribute("runModes", runModes);
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
        if (LogWebSocketHandle.SYSTEM_ID.equals(id)) {
            return JsonMessage.getString(401, "项目id " + LogWebSocketHandle.SYSTEM_ID + " 关键词被系统占用");
        }
        // 运行模式
        String runMode = getParameter("runMode");
        ProjectInfoModel.RunMode runMode1 = ProjectInfoModel.RunMode.ClassPath;
        try {
            runMode1 = ProjectInfoModel.RunMode.valueOf(runMode);
        } catch (Exception ignored) {
        }
        projectInfo.setRunMode(runMode1);
        //
        if (!whitelistDirectoryService.checkProjectDirectory(whitelistDirectory)) {
            return JsonMessage.getString(401, "请选择正确的项目路径,或者还没有配置白名单");
        }
        String lib = projectInfo.getLib();
        if (StrUtil.isEmpty(lib)) {
            return JsonMessage.getString(401, "项目lib不能为空");
        }
        if (StrUtil.SLASH.equals(lib)) {
            return JsonMessage.getString(401, "项目lib不能为顶级目录");
        }
        if (Validator.isChinese(lib)) {
            return JsonMessage.getString(401, "项目lib中不能包含中文");
        }
        if (!checkPathSafe(lib)) {
            return JsonMessage.getString(401, "项目lib存在提升目录问题");
        }

        lib = String.format("%s/%s", whitelistDirectory, lib);
        lib = FileUtil.normalize(lib);
        // 重复lib
        List<ProjectInfoModel> list = projectInfoService.list();
        if (list != null) {
            for (ProjectInfoModel projectInfoModel : list) {
                if (!projectInfoModel.getId().equals(id) && projectInfoModel.getLib().equals(lib)) {
                    return JsonMessage.getString(401, "当前项目lib已经被【" + projectInfoModel.getName() + "】占用,请检查");
                }
            }
        }
        projectInfo.setLib(lib);
        File checkFile = new File(projectInfo.getLib());
        if (checkFile.exists() && checkFile.isFile()) {
            return JsonMessage.getString(401, "项目lib是一个已经存在的文件");
        }

        String log = new File(lib).getParent();
        log = String.format("%s/%s.log", log, id);
        projectInfo.setLog(FileUtil.normalize(log));
        checkFile = new File(projectInfo.getLog());
        if (checkFile.exists() && checkFile.isDirectory()) {
            return JsonMessage.getString(401, "项目log是一个已经存在的文件夹");
        }
        //
        String token = projectInfo.getToken();
        if (!ProjectInfoModel.NO_TOKEN.equals(token)) {
            if (!ReUtil.isMatch(PatternPool.URL_HTTP, token)) {
                return JsonMessage.getString(401, "WebHooks 地址不合法");
            }
        }

        // 判断空格
        if (id.contains(StrUtil.SPACE) || lib.contains(StrUtil.SPACE) || log.contains(StrUtil.SPACE) || token.contains(StrUtil.SPACE)) {
            return JsonMessage.getString(401, "项目Id、项目Lib、WebHooks不能包含空格");
        }

        return save(projectInfo);
    }

    private String save(ProjectInfoModel projectInfo) throws IOException {
        ProjectInfoModel exits = projectInfoService.getItem(projectInfo.getId());
        try {
            UserModel userName = getUser();
            JsonMessage jsonMessage = checkPath(projectInfo);
            if (jsonMessage != null) {
                return jsonMessage.toString();
            }
            if (exits == null) {
                if (!userName.isManage()) {
                    return JsonMessage.getString(400, "管理员才能创建项目!");
                }
                projectInfo.setCreateTime(DateUtil.now());
                this.modify(projectInfo);
                projectInfoService.addItem(projectInfo);
                return JsonMessage.getString(200, "新增成功！");
            }
            if (!userName.isProject(projectInfo.getId())) {
                return JsonMessage.getString(400, "你没有对应操作权限操作!");
            }
            this.modify(exits);
            exits.setLog(projectInfo.getLog());
            exits.setName(projectInfo.getName());
            exits.setGroup(projectInfo.getGroup());
            exits.setMainClass(projectInfo.getMainClass());
            exits.setLib(projectInfo.getLib());
            exits.setJvm(projectInfo.getJvm());
            exits.setArgs(projectInfo.getArgs());
            exits.setBuildTag(projectInfo.getBuildTag());
            exits.setRunMode(projectInfo.getRunMode());
            //
            moveTo(exits, projectInfo);
            projectInfoService.updateProject(exits);
            return JsonMessage.getString(200, "修改成功");
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
            return JsonMessage.getString(500, e.getMessage());
        }
    }

    private void modify(ProjectInfoModel exits) {
        UserModel userName = getUser();
        // 隐藏系统管理员登录名
        if (userName.isSystemUser()) {
            exits.setModifyUser(UserModel.SYSTEM_OCCUPY_NAME);
        } else {
            exits.setModifyUser(userName.getId());
        }
    }

    /**
     * 验证lib 暂时用情况
     *
     * @param id     项目Id
     * @param newLib 新lib
     * @return json
     * @throws IOException IO
     */
    @RequestMapping(value = "judge_lib.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String saveProject(String id, String newLib) throws IOException {
        ProjectInfoModel exits = projectInfoService.getItem(id);
        File file = new File(newLib);
        String msg = null;
        if (exits == null) {
            if (file.exists() && file.isFile()) {
                msg = "lib目录当前是一个已经存在的文件,请修改";
            }
        } else {
            File oldLib = new File(exits.getLib());
            if (file.exists() && oldLib.exists()) {
                if (file.isFile()) {
                    msg = "lib目录当前是一个已经存在的文件,请修改";
                } else {
                    msg = "lib目录已经存在,保存将覆盖原文件夹并会自动同步原lib目录";
                }
            }
        }
        if (msg == null && Validator.isChinese(newLib)) {
            msg = "不建议使用中文目录";
        }
        if (msg == null) {
            return JsonMessage.getString(200, "");
        }
        return JsonMessage.getString(400, msg);
    }

    private void moveTo(ProjectInfoModel old, ProjectInfoModel news) {
        // 移动目录
        if (!old.getLib().equals(news.getLib())) {
            File oldLib = new File(old.getLib());
            if (oldLib.exists()) {
                File newsLib = new File(news.getLib());
                FileUtil.move(oldLib, newsLib, true);
            }
        }
        // log
        if (!old.getLog().equals(news.getLog())) {
            File oldLog = new File(old.getLog());
            if (oldLog.exists()) {
                File newsLog = new File(news.getLog());
                FileUtil.move(oldLog, newsLog, true);
            }
            // logBack
            File oldLogBack = old.getLogBack();
            if (oldLogBack.exists()) {
                FileUtil.move(oldLogBack, news.getLogBack(), true);
            }
        }

    }

    /**
     * 路径存在包含关系
     *
     * @param projectInfoModel 比较的项目
     * @return 不为null 则为错误
     */
    private JsonMessage checkPath(ProjectInfoModel projectInfoModel) {
        List<ProjectInfoModel> projectInfoModelList = projectInfoService.list();
        ProjectInfoModel projectInfoModel1 = null;
        for (ProjectInfoModel model : projectInfoModelList) {
            if (!model.getId().equals(projectInfoModel.getId())) {
                File file1 = new File(model.getLib());
                File file2 = new File(projectInfoModel.getLib());
                if (FileUtil.pathEquals(file1, file2)) {
                    projectInfoModel1 = model;
                    break;
                }
                // 包含关系
                if (pathContains(file1, file2) || pathContains(file2, file1)) {
                    projectInfoModel1 = model;
                    break;
                }
            }
        }
        if (projectInfoModel1 != null) {
            return new JsonMessage(401, "项目lib和【" + projectInfoModel1.getName() + "】项目冲突:" + projectInfoModel1.getLib());
        }
        return null;
    }

    private boolean pathContains(File file1, File file2) {
        try {
            return StrUtil.startWith(file1.getCanonicalPath() + File.separator, file2.getCanonicalPath() + File.separator, true);
        } catch (Exception e) {
            return StrUtil.startWith(file1.getAbsolutePath() + File.separator, file2.getAbsolutePath() + File.separator, true);
        }
    }
}
