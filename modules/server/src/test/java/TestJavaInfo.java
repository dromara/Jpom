import cn.hutool.system.SystemUtil;
import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import sun.management.ConnectorAddressLink;

import java.io.IOException;

/**
 * Created by jiangzeyin on 2019/3/20.
 */
public class TestJavaInfo {
    public static void main(String[] args) throws IOException, AttachNotSupportedException, AgentLoadException, AgentInitializationException {
        System.out.println(SystemUtil.getJavaRuntimeInfo().getHomeDir());
        VirtualMachine virtualMachine = VirtualMachine.attach("16772");
//        String agent = StrUtil.format("{}{}lib{}management-agent.jar", SystemUtil.getJavaRuntimeInfo().getHomeDir(), File.separator, File.separator);
//        virtualMachine.loadAgent(agent);
//        virtualMachine.loadAgent(agent);
        ConnectorAddressLink.export(String.valueOf(12044));
        System.out.println(ConnectorAddressLink.importFrom(12044));
    }
}
