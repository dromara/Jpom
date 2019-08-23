import cn.hutool.system.SystemUtil;
import io.jpom.util.JvmUtil;
import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;

import java.io.IOException;

/**
 * Created by jiangzeyin on 2019/3/20.
 */
public class TestJavaInfo {
    public static void main(String[] args) throws IOException, AttachNotSupportedException, AgentLoadException, AgentInitializationException, ClassNotFoundException {
        System.out.println(SystemUtil.getJavaRuntimeInfo().getHomeDir());
//        VirtualMachine virtualMachine = VirtualMachine.attach("16772");
//        String agent = StrUtil.format("{}{}lib{}management-agent.jar", SystemUtil.getJavaRuntimeInfo().getHomeDir(), File.separator, File.separator);
//        virtualMachine.loadAgent(agent);
//        virtualMachine.loadAgent(agent);
        JvmUtil.importFrom(7292);
        System.out.println(JvmUtil.importFrom(7292));
    }
}
