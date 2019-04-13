package cn.keepbx.jpom.util;

import cn.hutool.core.convert.Convert;
import cn.keepbx.jpom.system.init.CheckPath;
import com.sun.management.OperatingSystemMXBean;
import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.VirtualMachine;
import sun.management.ConnectorAddressLink;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

/**
 * jvm jmx 工具
 *
 * @author jiangzeyin
 * @date 2019/4/13
 */
public class JvmUtil {

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

    public static OperatingSystemMXBean getOperatingSystemMXBean(VirtualMachine virtualMachine) throws Exception {
        JMXServiceURL jmxServiceURL = getJMXServiceURL(virtualMachine);
        if (jmxServiceURL == null) {
            return null;
        }
        JMXConnector jmxConnector = JMXConnectorFactory.connect(jmxServiceURL, null);
        MBeanServerConnection mBeanServerConnection = jmxConnector.getMBeanServerConnection();
        return ManagementFactory.newPlatformMXBeanProxy(mBeanServerConnection, ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME, OperatingSystemMXBean.class);
    }

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
        String agent = CheckPath.getManagementAgent();
        virtualMachine.loadAgent(agent);
        address = virtualMachine.getAgentProperties().getProperty("com.sun.management.jmxremote.localConnectorAddress");
        if (address != null) {
            return new JMXServiceURL(address);
        }
        return null;
    }

}
