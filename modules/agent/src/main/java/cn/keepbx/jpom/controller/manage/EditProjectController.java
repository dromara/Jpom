package cn.keepbx.jpom.controller.manage;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.PatternPool;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseAgentController;
import cn.keepbx.jpom.common.commander.AbstractProjectCommander;
import cn.keepbx.jpom.model.Role;
import cn.keepbx.jpom.model.RunMode;
import cn.keepbx.jpom.model.data.ProjectInfoModel;
import cn.keepbx.jpom.service.WhitelistDirectoryService;
import cn.keepbx.jpom.service.manage.ProjectInfoService;
import cn.keepbx.jpom.socket.CommonSocketConfig;
import cn.keepbx.jpom.system.ConfigBean;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.File;
import java.nio.file.Path;
import java.util.List;

/**
 * 编辑项目
 *
 * @author jiangzeyin
 * @date 2019/4/16
 */
@RestController
@RequestMapping(value = "/manage/")
public class EditProjectController extends BaseAgentController {
    @Resource
    private WhitelistDirectoryService whitelistDirectoryService;
    @Resource
    private ProjectInfoService projectInfoService;

    /**
     * 基础检查
     *
     * @param projectInfo        项目实体
     * @param whitelistDirectory 白名单
     * @param previewData        预检查数据
     * @return null 检查正常
     */
    private String checkParameter(ProjectInfoModel projectInfo, String whitelistDirectory, boolean previewData) {
        String id = projectInfo.getId();
        if (StrUtil.isEmptyOrUndefined(id)) {
            return JsonMessage.getString(400, "项目id不能为空");
        }
        if (!Validator.isGeneral(id, 2, 20)) {
            return JsonMessage.getString(401, "项目id 长度范围2-20（英文字母 、数字和下划线）");
        }
        if (CommonSocketConfig.SYSTEM_ID.equals(id)) {
            return JsonMessage.getString(401, "项目id " + CommonSocketConfig.SYSTEM_ID + " 关键词被系统占用");
        }
        // 防止和Jpom冲突
        if (StrUtil.isNotEmpty(ConfigBean.getInstance().applicationTag) && ConfigBean.getInstance().applicationTag.equalsIgnoreCase(id)) {
            return JsonMessage.getString(401, "当前项目id已经被Jpom占用");
        }
        // 运行模式
        String runMode = getParameter("runMode");
        RunMode runMode1 = RunMode.ClassPath;
        try {
            runMode1 = RunMode.valueOf(runMode);
        } catch (Exception ignored) {
        }
        projectInfo.setRunMode(runMode1);
        // 监测
        if (runMode1 == RunMode.ClassPath) {
            if (StrUtil.isEmpty(projectInfo.getMainClass())) {
                return JsonMessage.getString(401, "ClassPath 模式 MainClass必填");
            }
        } else if (runMode1 == RunMode.Jar) {
            projectInfo.setMainClass("");
        }
        if (!previewData) {
            // 不是预检查数据才效验白名单
            if (!whitelistDirectoryService.checkProjectDirectory(whitelistDirectory)) {
                return JsonMessage.getString(401, "请选择正确的项目路径,或者还没有配置白名单");
            }
        }
        // 判断是否为分发添加
        String strOutGivingProject = getParameter("outGivingProject");
        boolean outGivingProject = Boolean.valueOf(strOutGivingProject);
        if (outGivingProject) {
            // 检查权限
            Role role = getUserRole();
            if (role != Role.System && role != Role.NodeManage) {
                return JsonMessage.getString(405, "没有权限操作分发项目管理");
            }
            // 检查白名单
            if (!whitelistDirectoryService.checkProjectDirectory(whitelistDirectory)) {
                if (role == Role.System) {
                    whitelistDirectoryService.addProjectWhiteList(whitelistDirectory);
                } else {
                    return JsonMessage.getString(405, "对应白名单还没有添加,请联系管理员添加");
                }
            }
        }
        String lib = projectInfo.getLib();
        if (StrUtil.isEmpty(lib) || StrUtil.SLASH.equals(lib) || Validator.isChinese(lib)) {
            return JsonMessage.getString(401, "项目Jar路径不能为空,不能为顶级目录,不能包含中文");
        }
        if (!checkPathSafe(lib)) {
            return JsonMessage.getString(401, "项目Jar路径存在提升目录问题");
        }
        return null;
    }


    @RequestMapping(value = "saveProject", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String saveProject(ProjectInfoModel projectInfo, String whitelistDirectory) {
        // 预检查数据
        String strPreviewData = getParameter("previewData");
        boolean previewData = Convert.toBool(strPreviewData, false);
        //
        String error = checkParameter(projectInfo, whitelistDirectory, previewData);
        if (error != null) {
            return error;
        }
        String id = projectInfo.getId();
        //
        String lib = projectInfo.getLib();
        lib = String.format("%s/%s", whitelistDirectory, lib);
        lib = FileUtil.normalize(lib);
        // 重复lib
        List<ProjectInfoModel> list = projectInfoService.list();
        if (list != null) {
            for (ProjectInfoModel projectInfoModel : list) {
                if (!projectInfoModel.getId().equals(id) && projectInfoModel.getLib().equals(lib)) {
                    return JsonMessage.getString(401, "当前项目Jar路径已经被【" + projectInfoModel.getName() + "】占用,请检查");
                }
            }
        }
        projectInfo.setLib(lib);
        File checkFile = new File(projectInfo.getLib());
        if (checkFile.exists() && checkFile.isFile()) {
            return JsonMessage.getString(401, "项目Jar路径是一个已经存在的文件");
        }
        // 自动生成log文件
        String log = new File(lib).getParent();
        log = String.format("%s/%s.log", log, id);
        projectInfo.setLog(FileUtil.normalize(log));
        checkFile = new File(projectInfo.getLog());
        if (checkFile.exists() && checkFile.isDirectory()) {
            return JsonMessage.getString(401, "项目log是一个已经存在的文件夹");
        }
        //
        String token = projectInfo.getToken();
        if (StrUtil.isNotEmpty(token) && !ReUtil.isMatch(PatternPool.URL_HTTP, token)) {
            return JsonMessage.getString(401, "WebHooks 地址不合法");
        }
        // 判断空格
        if (id.contains(StrUtil.SPACE) || lib.contains(StrUtil.SPACE)) {
            return JsonMessage.getString(401, "项目Id、项目Jar不能包含空格");
        }
        return save(projectInfo, previewData);
    }

    private String save(ProjectInfoModel projectInfo, boolean previewData) {
        String edit = getParameter("edit");
        ProjectInfoModel exits = projectInfoService.getItem(projectInfo.getId());
        try {
            JsonMessage jsonMessage = checkPath(projectInfo);
            if (jsonMessage != null) {
                return jsonMessage.toString();
            }
            if (exits == null) {
                // 检查运行中的tag 是否被占用
                if (AbstractProjectCommander.getInstance().isRun(projectInfo.getId())) {
                    return JsonMessage.getString(400, "当前项目id已经被正在运行的程序占用");
                }
                if (previewData) {
                    // 预检查数据
                    return JsonMessage.getString(200, "");
                } else {
                    projectInfo.setCreateTime(DateUtil.now());
                    this.modify(projectInfo);
                    projectInfoService.addItem(projectInfo);
                    return JsonMessage.getString(200, "新增成功！");
                }
            }
            //
            if (!"on".equalsIgnoreCase(edit)) {
                return JsonMessage.getString(400, "项目id已经存在啦");
            }
            if (previewData) {
                // 预检查数据
                return JsonMessage.getString(200, "");
            } else {
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
                exits.setToken(projectInfo.getToken());
                //
                moveTo(exits, projectInfo);
                projectInfoService.updateItem(exits);
                return JsonMessage.getString(200, "修改成功");
            }
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
            return JsonMessage.getString(500, e.getMessage());
        }
    }

    /**
     * 记录修改人
     *
     * @param exits 项目
     */
    private void modify(ProjectInfoModel exits) {
        exits.setModifyUser(getUserName());
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
                if (FileUtil.isSub(file1, file2) || FileUtil.isSub(file2, file1)) {
                    projectInfoModel1 = model;
                    break;
                }
            }
        }
        if (projectInfoModel1 != null) {
            return new JsonMessage(401, "项目Jar路径和【" + projectInfoModel1.getName() + "】项目冲突:" + projectInfoModel1.getLib());
        }
        return null;
    }


    @RequestMapping(value = "deleteProject", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String deleteProject() {
        ProjectInfoModel projectInfoModel = getProjectInfoModel();
        try {
            // 运行判断
            if (projectInfoModel.isStatus(true)) {
                return JsonMessage.getString(401, "不能删除正在运行的项目");
            }
            String userId = getUserName();
            projectInfoService.deleteProject(projectInfoModel, userId);
            return JsonMessage.getString(200, "删除成功！");
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
            return JsonMessage.getString(500, e.getMessage());
        }
    }

    @RequestMapping(value = "judge_lib.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String saveProject(String id, String newLib) {
        File file = new File(newLib);
        //  填写的jar路径是一个存在的文件
        if (file.exists() && file.isFile()) {
            return JsonMessage.getString(400, "填写jar目录当前是一个已经存在的文件,请修改");
        }
        ProjectInfoModel exits = projectInfoService.getItem(id);
        if (exits == null) {
            // 创建项目 填写的jar路径是已经存在的文件夹
            if (file.exists()) {
                return JsonMessage.getString(401, "填写jar目录当前已经在,创建成功后会自动同步文件");
            }
        } else {
            // 已经存在的项目
            File oldLib = new File(exits.getLib());
            Path newPath = file.toPath();
            Path oldPath = oldLib.toPath();
            if (newPath.equals(oldPath)) {
                // 新 旧没有变更
                return JsonMessage.getString(200, "");
            }
            if (file.exists()) {
                if (oldLib.exists()) {
                    // 新旧jar路径都存在，会自动覆盖新的jar路径中的文件
                    return JsonMessage.getString(401, "原jar目录已经存在并且新的jar目录已经存在,保存将覆盖新文件夹并会自动同步原jar目录");
                }
                return JsonMessage.getString(401, "填写jar目录当前已经在,创建成功后会自动同步文件");
            }
        }
        if (Validator.isChinese(newLib)) {
            return JsonMessage.getString(401, "不建议使用中文目录");
        }
        return JsonMessage.getString(200, "");
    }
}
