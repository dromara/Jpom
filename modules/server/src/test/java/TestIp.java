/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
import cn.hutool.core.lang.Filter;
import cn.hutool.core.net.NetUtil;
import org.junit.Test;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
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
}
