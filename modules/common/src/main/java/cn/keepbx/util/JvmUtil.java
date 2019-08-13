package cn.keepbx.util;

import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.keepbx.jpom.system.JpomRuntimeException;
import com.sun.management.OperatingSystemMXBean;
import com.sun.tools.attach.*;
import sun.jvmstat.monitor.*;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * jvm jmx 工具
 *
 * @author jiangzeyin
 * @date 2019/4/13
 */
public class JvmUtil {

    private static Method importFrom = null;

    static {
        int v = StrUtil.compareVersion(SystemUtil.getJavaInfo().getVersion(), "11.0");
        Class cls = null;
        try {
            if (v >= 0) {
                cls = Class.forName("jdk.internal.agent.ConnectorAddressLink");
            } else {
                cls = Class.forName("sun.management.ConnectorAddressLink");
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        if (cls != null) {
            importFrom = ReflectUtil.getMethod(cls, "importFrom", Integer.class);
        }
    }

    /**
     * 旧版jpom进程标记
     */
    private static final String OLD_JPOM_PID_TAG = "Dapplication";
    /**
     * 旧版jpom进程标记
     */
    private static final String OLD2_JPOM_PID_TAG = "Jpom.application";
    private static final String POM_PID_TAG = "DJpom.application";
    /**
     * 记录错误的进程信息，避免重复获取
     */
    public static final TimedCache<String, Boolean> PID_ERROR = new TimedCache<>(TimeUnit.DAYS.toMillis(1));

    /**
     * 获取进程标识
     *
     * @param id   id
     * @param path 路径
     * @return str
     */
    public static String getJpomPidTag(String id, String path) {
        return String.format("-%s=%s -DJpom.basedir=%s", POM_PID_TAG, id, path);
    }

    /**
     * 获取指定程序的jvm 信息
     *
     * @param jpomTag jpomTag
     * @return null 没有运行或者获取数据
     * @throws Exception 异常
     */
    public static MemoryMXBean getMemoryMXBean(String jpomTag) throws Exception {
        VirtualMachine virtualMachine = JvmUtil.getVirtualMachine(jpomTag);
        if (virtualMachine == null) {
            return null;
        }
        try {
            JMXServiceURL url = getJMXServiceURL(virtualMachine);
            if (url == null) {
                return null;
            }
            JMXConnector jmxConnector = JMXConnectorFactory.connect(url, null);
            MBeanServerConnection mBeanServerConnection = jmxConnector.getMBeanServerConnection();
            return ManagementFactory.newPlatformMXBeanProxy(mBeanServerConnection, ManagementFactory.MEMORY_MXBEAN_NAME, MemoryMXBean.class);
        } finally {
            virtualMachine.detach();
        }
    }

    /**
     * 获取指定程序的jvm 信息
     *
     * @param pId pId
     * @return 没有运行或者获取数据
     * @throws Exception 异常
     * @see OperatingSystemMXBean
     */
    public static OperatingSystemMXBean getOperatingSystemMXBean(String pId) throws Exception {
        VirtualMachine virtualMachine = getVirtualMachine(Integer.parseInt(pId));
        try {
            JMXServiceURL url = getJMXServiceURL(virtualMachine);
            if (url == null) {
                return null;
            }
            JMXConnector jmxConnector = JMXConnectorFactory.connect(url, null);
            MBeanServerConnection mBeanServerConnection = jmxConnector.getMBeanServerConnection();
            return ManagementFactory.newPlatformMXBeanProxy(mBeanServerConnection, ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME, OperatingSystemMXBean.class);
        } finally {
            virtualMachine.detach();
        }
    }

    /**
     * 获取指定程序的线程信息
     *
     * @param jpomTag jpomTag
     * @return 没有运行或者获取数据
     * @throws Exception 异常
     * @see ThreadMXBean
     */
    public static ThreadMXBean getThreadMXBean(String jpomTag) throws Exception {
        VirtualMachine virtualMachine = JvmUtil.getVirtualMachine(jpomTag);
        if (virtualMachine == null) {
            return null;
        }
        try {
            JMXServiceURL url = getJMXServiceURL(virtualMachine);
            if (url == null) {
                return null;
            }
            JMXConnector jmxConnector = JMXConnectorFactory.connect(url, null);
            MBeanServerConnection mBeanServerConnection = jmxConnector.getMBeanServerConnection();
            return ManagementFactory.newPlatformMXBeanProxy(mBeanServerConnection, ManagementFactory.THREAD_MXBEAN_NAME, ThreadMXBean.class);
        } finally {
            virtualMachine.detach();
        }
    }

    /**
     * 获取jmx 服务对象，如果没有加载则加载对应插件
     *
     * @param virtualMachine virtualMachine
     * @return JMXServiceURL
     * @throws IOException                  IO
     * @throws AgentLoadException           插件加载
     * @throws AgentInitializationException 插件初始化
     */
    private static JMXServiceURL getJMXServiceURL(VirtualMachine virtualMachine) throws IOException, AgentLoadException, AgentInitializationException, ClassNotFoundException {
        String address = virtualMachine.getAgentProperties().getProperty("com.sun.management.jmxremote.localConnectorAddress");
        if (address != null) {
            return new JMXServiceURL(address);
        }
        int pid = Convert.toInt(virtualMachine.id());
        address = importFrom(pid);
        if (address != null) {
            return new JMXServiceURL(address);
        }
        String agent = getManagementAgent();
        virtualMachine.loadAgent(agent);
        address = virtualMachine.getAgentProperties().getProperty("com.sun.management.jmxremote.localConnectorAddress");
        if (address != null) {
            return new JMXServiceURL(address);
        }
        return null;
    }

    public static String importFrom(int pid) {
        if (importFrom == null) {
            throw new JpomRuntimeException("jdk 环境不正常，没有找到：ConnectorAddressLink");
        }
        return ReflectUtil.invoke(null, importFrom, pid);
    }

    /**
     * 获取当前系统运行的java 程序个数
     *
     * @return 如果发生异常则返回0
     */
    public static int getJavaVirtualCount() {
        try {
            List<VirtualMachineDescriptor> descriptorList = VirtualMachine.list();
            return descriptorList.size();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 根据pid 获取jvm
     *
     * @param pid 进程id
     * @return VirtualMachine
     */
    public static VirtualMachine getVirtualMachine(int pid) {
        String pId = String.valueOf(pid);
        if (PID_ERROR.containsKey(pId)) {
            return null;
        }
        VirtualMachine virtualMachine = null;
        try {
            virtualMachine = VirtualMachine.attach(pId);
        } catch (AttachNotSupportedException | IOException e) {
            DefaultSystemLog.ERROR().error("获取jvm信息失败：" + pid, e);
            // 记录黑名单
            PID_ERROR.put(pId, true);
        }
        return virtualMachine;
    }

    /**
     * 工具Jpom运行项目的id 获取virtualMachine
     *
     * @param tag 项目id
     * @return VirtualMachine
     * @throws IOException 异常
     */
    public static VirtualMachine getVirtualMachine(String tag) throws IOException {
        // 通过VirtualMachine.list()列出所有的java进程
        List<VirtualMachineDescriptor> descriptorList = VirtualMachine.list();
        for (VirtualMachineDescriptor virtualMachineDescriptor : descriptorList) {
            String pid = virtualMachineDescriptor.id();
            if (PID_ERROR.containsKey(pid)) {
                continue;
            }
            VirtualMachine virtualMachine;
            try {
                virtualMachine = VirtualMachine.attach(virtualMachineDescriptor);
            } catch (AttachNotSupportedException | IOException e) {
                DefaultSystemLog.ERROR().error("获取jvm信息失败：" + pid, e);
                // 记录黑名单
                PID_ERROR.put(pid, true);
                continue;
            }
            if (checkVirtualMachineIsJpom(virtualMachine, tag)) {
                return virtualMachine;
            }
        }
        return null;
    }

    public static boolean checkVirtualMachineIsJpom(VirtualMachine virtualMachine, String tag) throws IOException {
        String appTag = String.format("-%s=%s ", JvmUtil.POM_PID_TAG, tag);
        String appTag2 = String.format("-%s=%s ", JvmUtil.OLD_JPOM_PID_TAG, tag);
        String appTag3 = String.format("-%s=%s", JvmUtil.OLD2_JPOM_PID_TAG, tag);
        Properties properties = virtualMachine.getAgentProperties();
        String args = properties.getProperty("sun.jvm.args", "");
        if (StrUtil.containsAnyIgnoreCase(args, appTag, appTag2, appTag3)) {
            return true;
        }
        args = properties.getProperty("sun.java.command", "");
        return StrUtil.containsAnyIgnoreCase(args, appTag, appTag2, appTag3);
    }

    /**
     * 获取jdk 中agent
     *
     * @return 路径
     */
    private static String getManagementAgent() {
        File file = FileUtil.file(SystemUtil.getJavaRuntimeInfo().getHomeDir(), "lib", "management-agent.jar");
        if (file.exists() && file.isFile()) {
            return file.getAbsolutePath();
        }
        throw new JpomRuntimeException("JDK中" + file.getAbsolutePath() + " 文件不存在");
    }

    /**
     * 获取jdk 中的tools jar文件路径
     *
     * @return file
     */
    public static File getToolsJar() {
        File file = new File(SystemUtil.getJavaRuntimeInfo().getHomeDir());
        return new File(file.getParentFile(), "lib/tools.jar");
    }

    /**
     * 工具指定的 mainClass 获取对应所有的的  MonitoredVm对象
     *
     * @param mainClass 程序运行主类
     * @return list
     * @throws MonitorException   e
     * @throws URISyntaxException e
     */
    public static List<MonitoredVm> listMainClass(String mainClass) throws MonitorException, URISyntaxException {
        List<MonitoredVm> monitoredVms = new ArrayList<>();
        MonitoredHost local = MonitoredHost.getMonitoredHost("localhost");
        // 取得所有在活动的虚拟机集合
        Set<?> vmList = new HashSet<Object>(local.activeVms());
        // 遍历集合，输出PID和进程名
        for (Object process : vmList) {
            MonitoredVm vm = local.getMonitoredVm(new VmIdentifier("//" + process));
            // 获取类名
            String processName = MonitoredVmUtil.mainClass(vm, true);
            if (!mainClass.equals(processName)) {
                continue;
            }
            monitoredVms.add(vm);
        }
        return monitoredVms;
    }
}
