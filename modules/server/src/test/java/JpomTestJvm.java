/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
//import com.sun.tools.attach.AttachNotSupportedException;
//import com.sun.tools.attach.VirtualMachine;
//import com.sun.tools.attach.VirtualMachineDescriptor;
//
//import java.io.IOException;
//import java.util.List;
//import java.util.Properties;
//
///**
// * Created by bwcx_jzy on 2019/4/19.
// */
//public class JpomTestJvm {
//
//    public static void main(String[] args) throws IOException, AttachNotSupportedException {
//        List<VirtualMachineDescriptor> descriptorList = VirtualMachine.list();
//        if (descriptorList.isEmpty()) {
//            System.out.println("没有任何结果");
//        }
//        for (VirtualMachineDescriptor virtualMachineDescriptor : descriptorList) {
//            // 根据虚拟机描述查询启动属性，如果属性-Dapplication匹配，说明项目已经启动，并返回进程id
//            VirtualMachine virtualMachine = VirtualMachine.attach(virtualMachineDescriptor);
//            Properties properties = virtualMachine.getAgentProperties();
//            System.out.println(properties);
//        }
//    }
//}
