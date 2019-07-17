package cn.keepbx.util;

import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.JavaRuntimeInfo;
import cn.hutool.system.SystemUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.keepbx.jpom.system.JpomRuntimeException;
import com.sun.management.OperatingSystemMXBean;
import com.sun.tools.attach.*;
import sun.jvmstat.monitor.*;
import sun.management.ConnectorAddressLink;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
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

    private static final JavaRuntimeInfo JAVA_RUNTIME_INFO = SystemUtil.getJavaRuntimeInfo();
    /**
     * 记录错误的进程信息，避免重复获取
     */
    private static final TimedCache<String, Boolean> PID_ERROR = new TimedCache<>(TimeUnit.DAYS.toMillis(1));

    /**
     * 获取指定程序的jvm 信息
     *
     * @param virtualMachine VirtualMachine
     * @return null 没有运行或者获取数据
     * @throws Exception 异常
     */
    public static MemoryMXBean getMemoryMXBean(VirtualMachine virtualMachine) throws Exception {
        JMXServiceURL jmxServiceURL = getJMXServiceURL(virtualMachine);
        if (jmxServiceURL == null) {
            return null;
        }
        JMXConnector jmxConnector = JMXConnectorFactory.connect(jmxServiceURL, null);
        MBeanServerConnection mBeanServerConnection = jmxConnector.getMBeanServerConnection();
        return ManagementFactory.newPlatformMXBeanProxy(mBeanServerConnection, ManagementFactory.MEMORY_MXBEAN_NAME, MemoryMXBean.class);
    }

    /**
     * 获取指定程序的jvm 信息
     *
     * @param virtualMachine VirtualMachine
     * @return 没有运行或者获取数据
     * @throws Exception 异常
     * @see OperatingSystemMXBean
     */
    public static OperatingSystemMXBean getOperatingSystemMXBean(VirtualMachine virtualMachine) throws Exception {
        JMXServiceURL jmxServiceURL = getJMXServiceURL(virtualMachine);
        if (jmxServiceURL == null) {
            return null;
        }
        JMXConnector jmxConnector = JMXConnectorFactory.connect(jmxServiceURL, null);
        MBeanServerConnection mBeanServerConnection = jmxConnector.getMBeanServerConnection();
        return ManagementFactory.newPlatformMXBeanProxy(mBeanServerConnection, ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME, OperatingSystemMXBean.class);
    }

    /**
     * 获取指定程序的线程信息
     *
     * @param virtualMachine VirtualMachine
     * @return 没有运行或者获取数据
     * @throws Exception 异常
     * @see ThreadMXBean
     */
    public static ThreadMXBean getThreadMXBean(VirtualMachine virtualMachine) throws Exception {
        JMXServiceURL jmxServiceURL = getJMXServiceURL(virtualMachine);
        if (jmxServiceURL == null) {
            return null;
        }
        JMXConnector jmxConnector = JMXConnectorFactory.connect(jmxServiceURL, null);
        MBeanServerConnection mBeanServerConnection = jmxConnector.getMBeanServerConnection();
        return ManagementFactory.newPlatformMXBeanProxy(mBeanServerConnection, ManagementFactory.THREAD_MXBEAN_NAME, ThreadMXBean.class);
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
    private static JMXServiceURL getJMXServiceURL(VirtualMachine virtualMachine) throws IOException, AgentLoadException, AgentInitializationException {
        String address = virtualMachine.getAgentProperties().getProperty("com.sun.management.jmxremote.localConnectorAddress");
        if (address != null) {
            return new JMXServiceURL(address);
        }
        int pid = Convert.toInt(virtualMachine.id());
        address = ConnectorAddressLink.importFrom(pid);
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
     * 工具Jpom运行项目的id 获取virtualMachine
     *
     * @param tag 项目id
     * @return VirtualMachine
     * @throws IOException 异常
     */
    public static VirtualMachine getVirtualMachine(String tag) throws IOException {
        // 添加空格是为了防止startWith
        String appTag = String.format("-Dapplication=%s ", tag);
        // 通过VirtualMachine.list()列出所有的java进程
        List<VirtualMachineDescriptor> descriptorList = VirtualMachine.list();
        for (VirtualMachineDescriptor virtualMachineDescriptor : descriptorList) {
            // 根据虚拟机描述查询启动属性，如果属性-Dapplication匹配，说明项目已经启动，并返回进程id
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
            Properties properties = virtualMachine.getAgentProperties();
            String args = properties.getProperty("sun.jvm.args", "");
            if (StrUtil.containsIgnoreCase(args, appTag)) {
                return virtualMachine;
            }
            args = properties.getProperty("sun.java.command", "");
            if (StrUtil.containsIgnoreCase(args, appTag)) {
                return virtualMachine;
            }
        }
        return null;
    }

    /**
     * 获取jdk 中agent
     *
     * @return 路径
     */
    private static String getManagementAgent() {
        String agent = StrUtil.format("{}{}lib{}management-agent.jar", JAVA_RUNTIME_INFO.getHomeDir(), File.separator, File.separator);
        File file = new File(agent);
        if (file.exists() && file.isFile()) {
            return agent;
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
