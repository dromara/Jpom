/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
import cn.hutool.core.net.Ipv4Util;
import cn.hutool.core.net.NetUtil;
import org.junit.Test;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.LinkedHashSet;

/**
 * @author bwcx_jzy
 * @since Created Time 2021/8/4
 */
public class TestIp {

    @Test
    public void test() {
        System.out.println(NetUtil.getLocalhostStr());
//		System.out.println(NetUtil.getLocalhost().getHostAddress());
        System.out.println("------");
        final LinkedHashSet<InetAddress> localAddressList = NetUtil.localAddressList(networkInterface -> {
            System.out.println(networkInterface.isVirtual());
            System.out.println(networkInterface.getIndex());
            return true;
        }, address -> {
            // 非loopback地址，指127.*.*.*的地址
            return !address.isLoopbackAddress()
                // 需为IPV4地址
                && address instanceof Inet4Address;
        });
        for (InetAddress inetAddress : localAddressList) {
            System.out.println(inetAddress.getHostAddress());
            //System.out.println(((Inet4Address) inetAddress).toString());
            //System.out.println("-");
        }
    }

    @Test
    public void test1() {
        String localHostName = NetUtil.getLocalHostName();
        System.out.println(localHostName);
    }

    @Test
    public void testConnect() {
        {
            InetSocketAddress address = NetUtil.createAddress("127.0.0.1", 2322);
            boolean open = NetUtil.isOpen(address, 5 * 1000);
            System.out.println(open);
        }
        {
            InetSocketAddress address = NetUtil.createAddress("baidu.com", 80);
            boolean open = NetUtil.isOpen(address, 5 * 1000);
            System.out.println(open);
        }
        {
            InetSocketAddress address = NetUtil.createAddress("baidu.com", 443);
            boolean open = NetUtil.isOpen(address, 5 * 1000);
            System.out.println(open);
        }
    }

    @Test
    public void testPublic() {
        long ipNum = Ipv4Util.ipv4ToLong("222.67.57.32");

        //
        long pBegin = Ipv4Util.ipv4ToLong("20.0.0.0");
        long pEnd = Ipv4Util.ipv4ToLong("223.255.255.255");
        System.out.println(isInner(ipNum, pBegin, pEnd));
    }

    @Test
    public void testPing() {
        boolean ping = NetUtil.ping("baidu.com", 2 * 1000);
        System.out.println(ping);
        System.out.println(NetUtil.ping("192.168.30.1", 2 * 1000));
    }

    @Test
    public void ICMPPing() {
        try {
            InetAddress address = InetAddress.getByName("192.168.30.1");
            Socket socket = new Socket(address, 0);

            // 发送Ping请求
            String request = "Ping请求";
            socket.getOutputStream().write(request.getBytes());

            // 接收Ping响应
            byte[] buffer = new byte[1024];
            int length = socket.getInputStream().read(buffer);
            String response = new String(buffer, 0, length);

            // 处理Ping响应
            System.out.println("Ping响应: " + response);

            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean isInner(long userIp, long begin, long end) {
        return (userIp >= begin) && (userIp <= end);
    }
}
