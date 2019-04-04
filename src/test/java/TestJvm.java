import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

/**
 * Created by jiangzeyin on 2019/4/4.
 */
public class TestJvm {
    public static void main(String[] args) throws IOException, AttachNotSupportedException {
        List<VirtualMachineDescriptor> descriptorList = VirtualMachine.list();
        for (VirtualMachineDescriptor virtualMachineDescriptor : descriptorList) {
            // 根据虚拟机描述查询启动属性，如果属性-Dapplication匹配，说明项目已经启动，并返回进程id
            VirtualMachine virtualMachine = VirtualMachine.attach(virtualMachineDescriptor);
            Properties properties = virtualMachine.getAgentProperties();
            System.out.println(properties);
        }
    }
}
