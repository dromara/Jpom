package cn.keepbx.jpom.model.data;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.keepbx.jpom.BaseJpomApplication;
import cn.keepbx.jpom.common.commander.AbstractProjectCommander;
import cn.keepbx.jpom.model.BaseModel;

import java.io.File;
import java.util.List;

/**
 * 项目配置信息实体
 *
 * @author jiangzeyin
 */
public class ProjectInfoModel extends BaseModel {
    private String name;
    /**
     * 分组
     */
    private String group;
    private String mainClass;
    private String lib;
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
    private String buildTag;
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
            return UserModel.SYSTEM_OCCUPY_NAME;
        }
        return modifyUser;
    }

    public void setModifyUser(String modifyUser) {
        this.modifyUser = modifyUser;
    }

    public void logModifyUser(UserModel userModel) {
        // 隐藏系统管理员登录名
        if (userModel.isSystemUser()) {
            this.setModifyUser(UserModel.SYSTEM_OCCUPY_NAME);
        } else {
            this.setModifyUser(userModel.getId());
        }
    }

    /**
     * 项目是否正在运行
     *
     * @param get 防止并发获取
     * @return true 正在运行
     */
    public boolean isStatus(boolean get) {
        if (!get) {
            return false;
        }
        try {
            status = AbstractProjectCommander.getInstance().isRun(getId());
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error("检查项目状态错误", e);
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

    public String getBuildTag() {
        return buildTag;
    }

    public void setBuildTag(String buildTag) {
        this.buildTag = buildTag;
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
        return jvm;
    }

    public void setJvm(String jvm) {
        this.jvm = jvm;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
        if (mainClass == null) {
            return "";
        }
        return mainClass;
    }

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public String getLib() {
        return lib;
    }

    public String getAbsoluteLib() {
        File file = new File(getLib());
        return file.getAbsolutePath();
    }

    /**
     * 获取项目文件中的所有jar 文件
     *
     * @param projectInfoModel 项目
     * @return list
     */
    public static List<File> listJars(ProjectInfoModel projectInfoModel) {
        File fileLib = new File(projectInfoModel.getLib());
        return FileUtil.loopFiles(fileLib, pathname -> {
            if (!pathname.isFile()) {
                return false;
            }
            return StrUtil.endWith(pathname.getName(), ".jar", true);
        });
    }

    /**
     * 拼接java 执行的jar路径
     *
     * @param projectInfoModel 项目
     * @return classpath 或者 jar
     */
    public static String getClassPathLib(ProjectInfoModel projectInfoModel) {
        List<File> files = listJars(projectInfoModel);
        if (files == null || files.size() <= 0) {
            return "";
        }
        // 获取lib下面的所有jar包
        StringBuilder classPath = new StringBuilder();
        RunMode runMode = projectInfoModel.getRunMode();
        int len = files.size();
        if (runMode == RunMode.ClassPath) {
            classPath.append("-classpath ");
        } else if (runMode == RunMode.Jar) {
            classPath.append("-jar ");
            // 只取一个jar文件
            len = 1;
        }
        for (int i = 0; i < len; i++) {
            File file = files.get(i);
            classPath.append(file.getAbsolutePath());
            if (i != len - 1) {
                classPath.append(BaseJpomApplication.OS_INFO.isWindows() ? ";" : ":");
            }
        }
        return classPath.toString();
    }

    public void setLib(String lib) {
        this.lib = lib;
    }

    public String getLog() {
        return log;
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
        return token;
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
        return args;
    }

    public void setArgs(String args) {
        this.args = args;
    }

    /**
     * 运行方式
     */
    public enum RunMode {
        /**
         * java -classpath
         */
        ClassPath,
        /**
         * java -jar
         */
        Jar,
    }
}
