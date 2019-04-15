package cn.keepbx.jpom.common.commander;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.JarClassLoader;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.system.OsInfo;
import cn.hutool.system.SystemUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.keepbx.jpom.common.commander.impl.LinuxCommander;
import cn.keepbx.jpom.common.commander.impl.WindowsCommander;
import cn.keepbx.jpom.model.NetstatModel;
import cn.keepbx.jpom.model.ProjectInfoModel;
import cn.keepbx.jpom.service.manage.CommandService;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

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
     * 查询出指定端口信息
     *
     * @param pid 进程id
     * @return 数组
     */
    public abstract List<NetstatModel> listNetstat(int pid);

    /**
     * 获取进程占用的主要端口
     *
     * @param pid 进程id
     * @return 端口
     */
    public String getMainPort(int pid) {
        List<NetstatModel> list = listNetstat(pid);
        if (list == null) {
            return "-";
        }
        int port = Integer.MAX_VALUE;
        for (NetstatModel model : list) {
            String local = model.getLocal();
            if (StrUtil.isEmpty(local)) {
                continue;
            }
            String[] ipPort = StrUtil.split(local, StrUtil.COLON);
            if (!"0.0.0.0".equals(ipPort[0])) {
                continue;
            }
            // 取最小的端口号
            int minPort = Convert.toInt(ipPort[1], Integer.MAX_VALUE);
            if (minPort < port) {
                port = minPort;
            }
        }
        if (port == Integer.MAX_VALUE) {
            return "-";
        }
        return String.valueOf(port);
    }

    /**
     * 停止
     *
     * @param projectInfoModel 项目
     * @return 结果
     * @throws Exception 异常
     */
    public String stop(ProjectInfoModel projectInfoModel) throws Exception {
        String tag = projectInfoModel.getId();
        String token = projectInfoModel.getToken();
        if (StrUtil.isNotEmpty(token)) {
            try {
                return HttpUtil.createGet(token).execute().body();
            } catch (Exception e) {
                return "get error";
            }
        }
        // 再次查看进程信息
        return status(tag);
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

    /**
     * 启动项目前基本检查
     *
     * @param projectInfoModel 项目
     * @return null 检查一切正常
     * @throws Exception 异常
     */
    protected String checkStart(ProjectInfoModel projectInfoModel) throws Exception {
        if (isRun(projectInfoModel.getId())) {
            return "运行中";
        }
        String lib = projectInfoModel.getLib();
        File fileLib = new File(lib);
        File[] files = fileLib.listFiles();
        if (files == null || files.length <= 0) {
            return "没有jar包,请先到文件管理中上传程序的jar";
        }
        //
        if (projectInfoModel.getRunMode() == ProjectInfoModel.RunMode.ClassPath) {
            JarClassLoader jarClassLoader = JarClassLoader.load(FileUtil.file(projectInfoModel.getLib()));
            // 判断主类
            try {
                jarClassLoader.loadClass(projectInfoModel.getMainClass());
            } catch (ClassNotFoundException notFound) {
                return "没有找到对应的MainClass:" + projectInfoModel.getMainClass();
            }
        } else {
            List<File> fileList = ProjectInfoModel.listJars(projectInfoModel);
            if (fileList == null || fileList.size() <= 0) {
                return "没有jar包,请先到文件管理中上传程序的jar";
            }
            File jarFile = fileList.get(0);
            try {
                JarFile jarFile1 = new JarFile(jarFile);
                Manifest manifest = jarFile1.getManifest();
                Attributes attributes = manifest.getMainAttributes();
                String mainClass = attributes.getValue("Main-Class");
                if (mainClass == null) {
                    return jarFile.getAbsolutePath() + "中没有找到对应的MainClass属性";
                }
                JarClassLoader jarClassLoader = JarClassLoader.load(jarFile);
                try {
                    jarClassLoader.loadClass(mainClass);
                } catch (ClassNotFoundException notFound) {
                    return jarFile.getAbsolutePath() + "中没有找到对应的MainClass:" + mainClass;
                }
            } catch (Exception e) {
                DefaultSystemLog.ERROR().error("解析jar", e);
                return jarFile.getAbsolutePath() + " 解析错误:" + e.getMessage();
            }
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
        if (StrUtil.isEmpty(projectInfoModel.getLog())) {
            return "ok";
        }
        File file = new File(projectInfoModel.getLog());
        if (!file.exists() || file.isDirectory()) {
            return "not exists";
        }
        // 文件内容太少不处理
        if (file.length() <= 1000) {
            return "ok";
        }
        File backPath = projectInfoModel.getLogBack();
        backPath = new File(backPath, DateTime.now().toString(DatePattern.PURE_DATETIME_FORMAT) + ".log");
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
        VirtualMachine virtualMachine = getVirtualMachine(tag);
        if (virtualMachine == null) {
            return CommandService.STOP_TAG;
        }
        return StrUtil.format("{}:{}", CommandService.RUNING_TAG, virtualMachine.id());
    }

    /**
     * 工具Jpom运行项目的id 获取virtualMachine
     *
     * @param tag 项目id
     * @return VirtualMachine
     * @throws IOException 异常
     */
    public VirtualMachine getVirtualMachine(String tag) throws IOException {
        // 添加空格是为了防止startWith
        tag = String.format("-Dapplication=%s ", tag);
        // 通过VirtualMachine.list()列出所有的java进程
        List<VirtualMachineDescriptor> descriptorList = VirtualMachine.list();
        for (VirtualMachineDescriptor virtualMachineDescriptor : descriptorList) {
            // 根据虚拟机描述查询启动属性，如果属性-Dapplication匹配，说明项目已经启动，并返回进程id
            VirtualMachine virtualMachine;
            try {
                virtualMachine = VirtualMachine.attach(virtualMachineDescriptor);
            } catch (AttachNotSupportedException e) {
                DefaultSystemLog.ERROR().error("获取jvm信息失败：" + virtualMachineDescriptor.id(), e);
                continue;
            }
            Properties properties = virtualMachine.getAgentProperties();
            String args = properties.getProperty("sun.jvm.args", "");
            if (StrUtil.containsIgnoreCase(args, tag)) {
                return virtualMachine;
            }
            args = properties.getProperty("sun.java.command", "");
            if (StrUtil.containsIgnoreCase(args, tag)) {
                return virtualMachine;
            }
        }
        return null;
    }


    /**
     * 获取进程id
     *
     * @param tag 项目Id
     * @return 未运行 返回 0
     * @throws Exception 异常
     */
    public int getPid(String tag) throws Exception {
        String result = status(tag);
        return parsePid(result);
    }

    /**
     * 转换pid
     *
     * @param result 查询信息
     * @return int
     */
    protected static int parsePid(String result) {
        if (result.startsWith(CommandService.RUNING_TAG)) {
            return Convert.toInt(result.split(":")[1]);
        }
        return 0;
    }

    /**
     * 是否正在运行
     *
     * @param tag id
     * @return true 正在运行
     * @throws Exception 异常
     */
    public boolean isRun(String tag) throws Exception {
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
        } while (count++ < 20);
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

    /**
     * 执行命令
     *
     * @param cmd 命令行
     * @return 结果
     * @throws IOException          IO
     * @throws InterruptedException 等待超时
     */
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
