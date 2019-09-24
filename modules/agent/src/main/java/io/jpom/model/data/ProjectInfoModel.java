package io.jpom.model.data;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.spring.SpringUtil;
import io.jpom.common.commander.AbstractProjectCommander;
import io.jpom.model.BaseModel;
import io.jpom.model.RunMode;
import io.jpom.service.WhitelistDirectoryService;
import io.jpom.system.JpomRuntimeException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 项目配置信息实体
 *
 * @author jiangzeyin
 */
public class ProjectInfoModel extends BaseModel {
    /**
     * 分组
     */
    private String group;
    private String mainClass;
    private String lib;
    /**
     * 白名单目录
     */
    private String whitelistDirectory;
    private String log;
    /**
     * jvm 参数
     */
    private String jvm;
    /**
     * WebHooks
     */
    private String token;
    private boolean status;
    private String createTime;
    private String modifyTime;
    private String args;
    /**
     * lib 目录当前文件状态
     */
    private String useLibDesc;
    /**
     * 当前运行lib 状态
     */
    private String runLibDesc;
    /**
     * 最后修改人
     */
    private String modifyUser;

    private RunMode runMode;
    /**
     * 节点分发项目，不允许在项目管理中编辑
     */
    private boolean outGivingProject;
    /**
     * 实际运行的命令
     */
    private String runCommand;

    public String getRunCommand() {
        return runCommand;
    }

    public void setRunCommand(String runCommand) {
        this.runCommand = runCommand;
    }

    public boolean isOutGivingProject() {
        return outGivingProject;
    }

    public void setOutGivingProject(boolean outGivingProject) {
        this.outGivingProject = outGivingProject;
    }

    public RunMode getRunMode() {
        if (runMode == null) {
            return RunMode.ClassPath;
        }
        return runMode;
    }

    public void setRunMode(RunMode runMode) {
        this.runMode = runMode;
    }

    public String getModifyUser() {
        if (StrUtil.isEmpty(modifyUser)) {
            return StrUtil.DASHED;
        }
        return modifyUser;
    }

    public void setModifyUser(String modifyUser) {
        this.modifyUser = modifyUser;
    }

    /**
     * 项目是否正在运行
     *
     * @param get 防止自动获取
     * @return true 正在运行
     */
    public boolean isStatus(boolean get) {
        try {
            status = AbstractProjectCommander.getInstance().isRun(getId());
        } catch (Exception e) {
            DefaultSystemLog.getLog().error("检查项目状态错误", e);
            status = false;
        }
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getRunLibDesc() {
        return runLibDesc;
    }

    public void setRunLibDesc(String runLibDesc) {
        this.runLibDesc = runLibDesc;
    }

    public String getUseLibDesc() {
        return useLibDesc;
    }

    public void setUseLibDesc(String useLibDesc) {
        this.useLibDesc = useLibDesc;
    }

    public String getModifyTime() {
        return modifyTime;
    }

    /**
     * 修改时间
     *
     * @param modifyTime time
     */
    public void setModifyTime(String modifyTime) {
        this.modifyTime = modifyTime;
    }

    public String getJvm() {
        return StrUtil.emptyToDefault(jvm, StrUtil.EMPTY);
    }

    public void setJvm(String jvm) {
        this.jvm = jvm;
    }

    public String getGroup() {
        if (StrUtil.isEmpty(group)) {
            return "默认";
        }
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getMainClass() {
        return StrUtil.emptyToDefault(mainClass, StrUtil.EMPTY);
    }

    private void repairWhitelist() {
        if (StrUtil.isEmpty(whitelistDirectory) && StrUtil.isEmpty(lib)) {
            throw new JpomRuntimeException("当前项目lib数据异常");
        }
        if (StrUtil.isNotEmpty(whitelistDirectory)) {
            return;
        }
        WhitelistDirectoryService whitelistDirectoryService = SpringUtil.getBean(WhitelistDirectoryService.class);
        List<String> project = whitelistDirectoryService.getWhitelist().getProject();
        for (String path : project) {
            if (lib.startsWith(path)) {
                String itemWhitelistDirectory = lib.substring(0, path.length());
                lib = lib.substring(path.length());
                setWhitelistDirectory(itemWhitelistDirectory);
                setLib(lib);
            }
        }
    }

    public String getWhitelistDirectory() {
        this.repairWhitelist();
        if (StrUtil.isEmpty(whitelistDirectory)) {
            throw new JpomRuntimeException("修护白名单数据异常");
        }
        return whitelistDirectory;
    }

    public void setWhitelistDirectory(String whitelistDirectory) {
        this.whitelistDirectory = whitelistDirectory;
    }

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public String getLib() {
        this.repairWhitelist();
        return lib;
    }

    public String allLib() {
        return FileUtil.file(getWhitelistDirectory(), getLib()).getAbsolutePath();
    }

    /**
     * 获取项目文件中的所有jar 文件
     *
     * @param projectInfoModel 项目
     * @return list
     */
    public static List<File> listJars(ProjectInfoModel projectInfoModel) {
        File fileLib = new File(projectInfoModel.allLib());
        File[] files = fileLib.listFiles();
        List<File> files1 = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                if (!file.isFile()) {
                    continue;
                }
                if (projectInfoModel.getRunMode() == RunMode.ClassPath || projectInfoModel.getRunMode() == RunMode.Jar) {
                    if (!StrUtil.endWith(file.getName(), FileUtil.JAR_FILE_EXT, true)) {
                        continue;
                    }
                } else if (projectInfoModel.getRunMode() == RunMode.War) {
                    if (!StrUtil.endWith(file.getName(), "war", true)) {
                        continue;
                    }
                }
                files1.add(file);
            }
        }
        return files1;
    }

    /**
     * 拼接java 执行的jar路径
     *
     * @param projectInfoModel 项目
     * @return classpath 或者 jar
     */
    public static String getClassPathLib(ProjectInfoModel projectInfoModel) {
        List<File> files = listJars(projectInfoModel);
        if (files.size() <= 0) {
            return "";
        }
        // 获取lib下面的所有jar包
        StringBuilder classPath = new StringBuilder();
        RunMode runMode = projectInfoModel.getRunMode();
        int len = files.size();
        if (runMode == RunMode.ClassPath) {
            classPath.append("-classpath ");
        } else if (runMode == RunMode.Jar || runMode == RunMode.War) {
            classPath.append("-jar ");
            // 只取一个jar文件
            len = 1;
        }
        for (int i = 0; i < len; i++) {
            File file = files.get(i);
            classPath.append(file.getAbsolutePath());
            if (i != len - 1) {
                classPath.append(SystemUtil.getOsInfo().isWindows() ? ";" : ":");
            }
        }
        return classPath.toString();
    }

    public void setLib(String lib) {
        this.lib = lib;
    }

    public String getLog() {
        return StrUtil.emptyToDefault(log, StrUtil.EMPTY);
    }

    public String getAbsoluteLog() {
        File file = new File(getLog());
        return file.getAbsolutePath();
    }

    public File getLogBack() {
        return new File(getLog() + "_back");
    }

    public void setLog(String log) {
        this.log = log;
    }

    /**
     * 默认
     *
     * @return url token
     */
    public String getToken() {
        // 兼容旧数据
        if ("no".equalsIgnoreCase(this.token)) {
            return "";
        }
        return StrUtil.emptyToDefault(token, StrUtil.EMPTY);
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getArgs() {
        return StrUtil.emptyToDefault(args, StrUtil.EMPTY);
    }

    public void setArgs(String args) {
        this.args = args;
    }
}
