package cn.keepbx.jpom.common.commander;

import cn.hutool.cache.impl.LRUCache;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.JarClassLoader;
import cn.hutool.core.text.StrSpliter;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.jpom.BaseJpomApplication;
import cn.keepbx.jpom.common.commander.impl.LinuxProjectCommander;
import cn.keepbx.jpom.common.commander.impl.WindowsProjectCommander;
import cn.keepbx.jpom.model.data.ProjectInfoModel;
import cn.keepbx.jpom.model.system.NetstatModel;
import cn.keepbx.jpom.service.manage.ProjectInfoService;
import cn.keepbx.jpom.system.JpomRuntimeException;
import cn.keepbx.jpom.util.CommandUtil;
import cn.keepbx.jpom.util.JvmUtil;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * 项目命令执行基类
 *
 * @author Administrator
 */
public abstract class AbstractProjectCommander {

    public static final String RUNING_TAG = "running";
    public static final String STOP_TAG = "stopped";

    private static AbstractProjectCommander abstractProjectCommander = null;


    /**
     * 进程id 对应Jpom 名称
     */
    private static final ConcurrentHashMap<Integer, String> PID_JPOM_NAME = new ConcurrentHashMap<>();
    /**
     * 进程Id 获取端口号
     */
    private static final LRUCache<Integer, Integer> PID_PORT = new LRUCache<>(100, TimeUnit.MINUTES.toMillis(10));

    /**
     * 实例化Commander
     *
     * @return 命令执行对象
     */
    public static AbstractProjectCommander getInstance() {
        if (abstractProjectCommander != null) {
            return abstractProjectCommander;
        }
        if (BaseJpomApplication.OS_INFO.isLinux()) {
            // Linux系统
            abstractProjectCommander = new LinuxProjectCommander();
        } else if (BaseJpomApplication.OS_INFO.isWindows()) {
            // Windows系统
            abstractProjectCommander = new WindowsProjectCommander();
        } else {
            throw new JpomRuntimeException("不支持的：" + BaseJpomApplication.OS_INFO.getName());
        }
        return abstractProjectCommander;
    }

    //---------------------------------------------------- 基本操作----start

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
                String body = HttpUtil.createGet(token).execute().body();
                DefaultSystemLog.LOG().info(projectInfoModel.getName() + ":" + body);
            } catch (Exception e) {
                DefaultSystemLog.ERROR().error("WebHooks 调用错误", e);
                return "WebHooks error:" + e.getMessage();
            }
        }
        // 再次查看进程信息
        String result = status(tag);
        //
        int pid = parsePid(result);
        if (pid > 0) {
            // 清空名称缓存
            PID_JPOM_NAME.remove(pid);
            // 端口号缓存
            PID_PORT.remove(pid);
        }
        return result;
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
     */
    public String backLog(ProjectInfoModel projectInfoModel) {
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
        if (BaseJpomApplication.OS_INFO.isLinux()) {
            CommandUtil.execCommand("cp /dev/null " + projectInfoModel.getLog());
        } else if (BaseJpomApplication.OS_INFO.isWindows()) {
            // 清空日志
            String r = CommandUtil.execSystemCommand("echo  \"\" > " + file.getAbsolutePath());
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
        VirtualMachine virtualMachine = JvmUtil.getVirtualMachine(tag);
        if (virtualMachine == null) {
            return AbstractProjectCommander.STOP_TAG;
        }
        return StrUtil.format("{}:{}", AbstractProjectCommander.RUNING_TAG, virtualMachine.id());
    }

    //---------------------------------------------------- 基本操作----end

    /**
     * 获取进程占用的主要端口
     *
     * @param pid 进程id
     * @return 端口
     */
    public String getMainPort(int pid) {
        Integer cachePort = PID_PORT.get(pid);
        if (cachePort != null) {
            return cachePort.toString();
        }
        List<NetstatModel> list = listNetstat(pid);
        if (list == null) {
            return StrUtil.DASHED;
        }
        int port = Integer.MAX_VALUE;
        for (NetstatModel model : list) {
            String local = model.getLocal();
            String portStr = getPortFormLocalIp(local);
            if (portStr == null) {
                continue;
            }
            // 取最小的端口号
            int minPort = Convert.toInt(portStr, Integer.MAX_VALUE);
            if (minPort < port) {
                port = minPort;
            }
        }
        if (port == Integer.MAX_VALUE) {
            return StrUtil.DASHED;
        }
        // 缓存
        PID_PORT.put(pid, port);
        return String.valueOf(port);
    }

    /**
     * 判断ip 信息是否为本地ip
     *
     * @param local ip信息
     * @return true 是本地ip
     */
    private String getPortFormLocalIp(String local) {
        if (StrUtil.isEmpty(local)) {
            return null;
        }
        List<String> ipPort = StrSpliter.splitTrim(local, StrUtil.COLON, true);
        if (ipPort.isEmpty()) {
            return null;
        }
        if ("0.0.0.0".equals(ipPort.get(0)) || ipPort.size() == 1) {
            // 0.0.0.0:8084  || :::18000
            return ipPort.get(ipPort.size() - 1);
        }
        return null;
    }


    /**
     * 根据指定进程id获取Jpom 名称
     *
     * @param pid 进程id
     * @return false 不是来自Jpom
     * @throws IOException 异常
     */
    public String getJpomNameByPid(int pid) throws IOException {
        String name = PID_JPOM_NAME.get(pid);
        if (name != null) {
            return name;
        }
        ProjectInfoService projectInfoService = SpringUtil.getBean(ProjectInfoService.class);
        List<ProjectInfoModel> projectInfoModels = projectInfoService.list();
        if (projectInfoModels == null || projectInfoModels.isEmpty()) {
            return StrUtil.DASHED;
        }
        VirtualMachine virtualMachine;
        try {
            virtualMachine = VirtualMachine.attach(String.valueOf(pid));
        } catch (AttachNotSupportedException | IOException e) {
            DefaultSystemLog.ERROR().error("获取jvm信息失败：" + pid, e);
            return StrUtil.DASHED;
        }
        Properties properties = virtualMachine.getAgentProperties();
        String appTag;
        for (ProjectInfoModel projectInfoModel : projectInfoModels) {
            appTag = String.format("-Dapplication=%s ", projectInfoModel.getId());
            String args = properties.getProperty("sun.jvm.args", "");
            if (StrUtil.containsIgnoreCase(args, appTag)) {
                name = projectInfoModel.getName();
                break;
            }
            args = properties.getProperty("sun.java.command", "");
            if (StrUtil.containsIgnoreCase(args, appTag)) {
                name = projectInfoModel.getName();
                break;
            }
        }
        if (name != null) {
            PID_JPOM_NAME.put(pid, name);
            return name;
        }
        return StrUtil.DASHED;
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
        if (result.startsWith(AbstractProjectCommander.RUNING_TAG)) {
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
        return result.contains(AbstractProjectCommander.RUNING_TAG);
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
}
