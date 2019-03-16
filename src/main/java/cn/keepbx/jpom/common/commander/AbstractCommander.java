package cn.keepbx.jpom.common.commander;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.system.OsInfo;
import cn.hutool.system.SystemUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.keepbx.jpom.common.commander.impl.LinuxCommander;
import cn.keepbx.jpom.common.commander.impl.WindowsCommander;
import cn.keepbx.jpom.model.ProjectInfoModel;
import cn.keepbx.jpom.service.manage.CommandService;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * 命令执行基类
 *
 * @author Administrator
 */
public abstract class AbstractCommander {

    private static AbstractCommander abstractCommander = null;
    protected Charset charset;
    public static final OsInfo OS_INFO = SystemUtil.getOsInfo();

    protected AbstractCommander(Charset charset) {
        this.charset = charset;
    }

    public Charset getCharset() {
        return charset;
    }

    /**
     * 实例化Commander
     *
     * @return 命令执行对象
     */
    public static AbstractCommander getInstance() {
        if (abstractCommander != null) {
            return abstractCommander;
        }

        if (OS_INFO.isLinux()) {
            // Linux系统
            abstractCommander = new LinuxCommander(CharsetUtil.CHARSET_UTF_8);
        } else if (OS_INFO.isWindows()) {
            // Windows系统
            abstractCommander = new WindowsCommander(CharsetUtil.CHARSET_GBK);
        } else {
            throw new RuntimeException("不支持的：" + OS_INFO.getName());
        }
        return abstractCommander;
    }

    /**
     * 启动
     *
     * @param projectInfoModel 项目
     * @return 结果
     * @throws Exception 异常
     */
    public abstract String start(ProjectInfoModel projectInfoModel) throws Exception;

    /**
     * 停止
     *
     * @param projectInfoModel 项目
     * @return 结果
     * @throws Exception 异常
     */
    public String stop(ProjectInfoModel projectInfoModel) throws Exception {
        String token = projectInfoModel.getToken();
        if (StrUtil.isNotEmpty(token) && !ProjectInfoModel.NO_TOKEN.equalsIgnoreCase(token)) {
            try {
                return HttpUtil.createGet(token).execute().body();
            } catch (Exception e) {
                return "get error";
            }
        }
        return "";
    }

    /**
     * 重启
     *
     * @param projectInfoModel 项目
     * @return 结果
     * @throws Exception 异常
     */
    public String restart(ProjectInfoModel projectInfoModel) throws Exception {
        if (isRun(projectInfoModel.getId())) {
            stop(projectInfoModel);
        }
        return start(projectInfoModel);
    }

    protected String checkStart(ProjectInfoModel projectInfoModel) throws Exception {
        if (isRun(projectInfoModel.getId())) {
            return "运行中";
        }
        String lib = projectInfoModel.getLib();
        File fileLib = new File(lib);
        File[] files = fileLib.listFiles();
        if (files == null) {
            return "没有jar包";
        }
        // 备份日志
        backLog(projectInfoModel);
        return null;
    }

    /**
     * 清空日志信息
     *
     * @param projectInfoModel 项目
     * @return 结果
     * @throws Exception 异常
     */
    public String backLog(ProjectInfoModel projectInfoModel) throws Exception {
        File file = new File(projectInfoModel.getLog());
        if (!file.exists()) {
            return "not exists";
        }
        File backPath = projectInfoModel.getLogBack();
        backPath = new File(backPath, file.getName() + "-" + DateTime.now().toString(DatePattern.PURE_DATETIME_FORMAT) + ".log");
        FileUtil.copy(file, backPath, true);
        if (OS_INFO.isLinux()) {
            execCommand("cp /dev/null " + projectInfoModel.getLog());
        } else if (OS_INFO.isWindows()) {
            // 清空日志
            String r = execSystemCommand("echo  \"\" > " + file.getAbsolutePath());
            if (StrUtil.isEmpty(r)) {
                DefaultSystemLog.LOG().info(r);
            }
        }
        return "ok";
    }

    /**
     * 查看状态
     *
     * @return 查询结果
     */
    public String status(String tag) throws Exception {
        tag = String.format("-Dapplication=%s", tag);
        String result = CommandService.STOP_TAG;
        // 通过VirtualMachine.list()列出所有的java进程
        List<VirtualMachineDescriptor> listvm = VirtualMachine.list();
        for (VirtualMachineDescriptor vmd : listvm) {
            // 根据进程id查询启动属性，如果属性-Dapplication匹配，说明项目已经启动，并返回进程id
            Properties properties = VirtualMachine.attach(vmd.id()).getAgentProperties();
            String args = StrUtil.emptyToDefault(properties.getProperty("sun.jvm.args"), "");
            if (StrUtil.containsIgnoreCase(args, tag)) {
                result = StrUtil.format("{}:{}", CommandService.RUNING_TAG, vmd.id());
                break;
            }
        }
        return result;
    }

    /**
     * 获取进程id
     *
     * @param tag 项目Id
     * @return 未运行 返回 0
     * @throws Exception 异常
     */
    public String getPid(String tag) throws Exception {
        String result = status(tag);
        if (result.startsWith(CommandService.RUNING_TAG)) {
            return result.split(":")[1];
        }
        return "0";
    }

    private boolean isRun(String tag) throws Exception {
        String result = status(tag);
        return result.contains(CommandService.RUNING_TAG);
    }

    /***
     * 阻塞检查程序状态
     * @param tag 程序tag
     * @param status 要检查的状态
     * @throws Exception E
     */
    protected void loopCheckRun(String tag, boolean status) throws Exception {
        int count = 0;
        do {
            if (isRun(tag) == status) {
                return;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }
        } while (count++ < 10);
    }

    public String execCommand(String command) throws Exception {
        return exec(new String[]{command});
    }

    public String execSystemCommand(String command) {
        String result = "error";
        try {
            String[] cmd;
            if (OS_INFO.isLinux()) {
                //执行linux系统命令
                cmd = new String[]{"/bin/sh", "-c", command};
            } else {
                cmd = new String[]{"cmd", "/c", command};
            }
            result = exec(cmd);
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error("执行命令异常", e);
            result += e.getMessage();
        }
        return result;
    }

    private String exec(String[] cmd) throws IOException, InterruptedException {
        DefaultSystemLog.LOG().info(Arrays.toString(cmd));
        String result;
        Process process;
        if (cmd.length == 1) {
            process = Runtime.getRuntime().exec(cmd[0]);
        } else {
            process = Runtime.getRuntime().exec(cmd);
        }
        InputStream is;
        int wait = process.waitFor();
        if (wait == 0) {
            is = process.getInputStream();
        } else {
            is = process.getErrorStream();
        }
        result = IoUtil.read(is, charset);
        is.close();
        process.destroy();
        if (StrUtil.isEmpty(result) && wait != 0) {
            result = "没有返回任何执行信息:" + wait;
        }
        return result;
    }

}
