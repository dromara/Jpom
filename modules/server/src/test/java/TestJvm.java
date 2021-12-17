/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
///*
// * The MIT License (MIT)
// *
// * Copyright (c) 2019 码之科技工作室
// *
// * Permission is hereby granted, free of charge, to any person obtaining a copy of
// * this software and associated documentation files (the "Software"), to deal in
// * the Software without restriction, including without limitation the rights to
// * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
// * the Software, and to permit persons to whom the Software is furnished to do so,
// * subject to the following conditions:
// *
// * The above copyright notice and this permission notice shall be included in all
// * copies or substantial portions of the Software.
// *
// * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
// * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
// * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
// * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
// * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
// */
//import cn.hutool.system.SystemUtil;
//import com.sun.tools.attach.AttachNotSupportedException;
//import sun.jvmstat.monitor.*;
//
//import java.io.IOException;
//import java.lang.management.ManagementFactory;
//import java.lang.management.RuntimeMXBean;
//import java.net.URISyntaxException;
//import java.util.HashSet;
//import java.util.Set;
//
///**
// * Created by jiangzeyin on 2019/4/4.
// */
//public class TestJvm {
//    public static void main(String[] args) throws IOException, AttachNotSupportedException, MonitorException, URISyntaxException {
////        System.out.println(getPid());
////        List<VirtualMachineDescriptor> descriptorList = VirtualMachine.list();
////        for (VirtualMachineDescriptor virtualMachineDescriptor : descriptorList) {
////            // 根据虚拟机描述查询启动属性，如果属性-Dapplication匹配，说明项目已经启动，并返回进程id
////            VirtualMachine virtualMachine = VirtualMachine.attach(virtualMachineDescriptor);
////            Properties properties = virtualMachine.getAgentProperties();
////            System.out.println(properties);
////        }
//        System.out.println(SystemUtil.getJavaRuntimeInfo().getVersion());
//
//        // 获取监控主机
//        MonitoredHost local = MonitoredHost.getMonitoredHost("localhost");
//        // 取得所有在活动的虚拟机集合
//        Set<?> vmlist = new HashSet<Object>(local.activeVms());
//        // 遍历集合，输出PID和进程名
//        for (Object process : vmlist) {
//            MonitoredVm vm = local.getMonitoredVm(new VmIdentifier("//" + process));
//            // 获取类名
//            String processname = MonitoredVmUtil.mainClass(vm, true);
//            System.out.println(processname);
//            if (!"io.jpom.JpomAgentApplication".equals(processname)) {
//                continue;
//            }
//            System.out.println(vm.getVmIdentifier().getUserInfo());
//            System.out.println(vm.getVmIdentifier().toString());
//        }
//    }
//
//    public static int getPid() {
//        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
//        System.out.println(runtime);
//        String name = runtime.getName(); // format: "pid@hostname"
//        System.out.println(name);
//        try {
//            return Integer.parseInt(name.substring(0, name.indexOf('@')));
//        } catch (Exception e) {
//            return -1;
//        }
//    }
//}
