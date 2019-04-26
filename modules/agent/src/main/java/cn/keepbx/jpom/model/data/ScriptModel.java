package cn.keepbx.jpom.model.data;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.BaseJpomApplication;
import cn.keepbx.jpom.model.BaseModel;
import cn.keepbx.jpom.system.AgentConfigBean;
import cn.keepbx.jpom.util.CommandUtil;

import java.io.File;

/**
 * 脚本模板
 *
 * @author jiangzeyin
 * @date 2019/4/24
 */
public class ScriptModel extends BaseModel {
    /**
     * 文件后缀
     */
    private static final String SUFFIX;

    static {
        if (BaseJpomApplication.OS_INFO.isWindows()) {
            SUFFIX = "bat";
        } else {
            SUFFIX = "sh";
        }
    }

    /**
     * 最后执行人员
     */
    private String lastRunUser;
    /**
     * 最后修改时间
     */
    private String modifyTime;
    /**
     * 脚本内容
     */
    private String context;

    public String getLastRunUser() {
        return StrUtil.emptyToDefault(lastRunUser, StrUtil.DASHED);
    }

    public void setLastRunUser(String lastRunUser) {
        this.lastRunUser = lastRunUser;
    }

    public String getModifyTime() {
        return modifyTime;
    }

    public void setModifyTime(String modifyTime) {
        this.modifyTime = modifyTime;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public File getFile(boolean get) {
        if (StrUtil.isEmpty(getId())) {
            throw new IllegalArgumentException("id 为空");
        }
        File path = AgentConfigBean.getInstance().getScriptPath();
        return FileUtil.file(path, getId(), "script." + SUFFIX);
    }

    public File getLogFile(boolean get) {
        if (StrUtil.isEmpty(getId())) {
            throw new IllegalArgumentException("id 为空");
        }
        File path = AgentConfigBean.getInstance().getScriptPath();
        File logFile;
        int count = 0;
        do {
            String now = DateTime.now().toString(DatePattern.PURE_DATETIME_PATTERN);
            logFile = FileUtil.file(path, getId(), "log", now + count + ".log");
            count++;
        } while (FileUtil.exist(logFile));
        return logFile;
    }

    public void saveFile() {
        File file = getFile(true);
        FileUtil.writeString(getContext(), file, CharsetUtil.CHARSET_UTF_8);
        // 添加权限
        if (BaseJpomApplication.OS_INFO.isLinux()) {
            CommandUtil.execCommand("chmod 755 " + FileUtil.getAbsolutePath(file));
        }
    }

    /**
     * 读取文件信息
     */
    public void readFileTime() {
        File file = getFile(true);
        long lastModified = file.lastModified();
        setModifyTime(DateUtil.date(lastModified).toString());

    }

    public void readFileContext() {
        File file = getFile(true);
        if (FileUtil.exist(file)) {
            //
            String context = FileUtil.readString(file, CharsetUtil.CHARSET_UTF_8);
            setContext(context);
        }
    }
}
