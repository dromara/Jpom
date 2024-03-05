/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package cert;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;

import java.io.InputStream;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class CertUtil {
    static {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    public static String getSubjectDN(InputStream bIn) {
        String dn = "";
        try {

//            BouncyCastleProvider provider = new BouncyCastleProvider();
//            CertificateFactory cf = CertificateFactory.getInstance("X509",
//                    provider);
            CertificateFactory cf = CertificateFactory.getInstance("X.509",
                "SUN");
            //android 需采用bcprov
//            CertificateFactory cf = CertificateFactory.getInstance("X.509",
//                    "BC");
            X509Certificate cert = (X509Certificate) cf
                .generateCertificate(bIn);
            dn = cert.getSubjectDN().getName();
            bIn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dn;
    }

    public static String parseCertDN(String dn, String type) {
        type = type + "=";
        String[] split = dn.split(StrUtil.COMMA);
        for (String x : split) {
            if (x.contains(type)) {
                x = x.trim();
                return x.substring(type.length());
            }
        }
        return null;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        InputStream inputStream = ResourceUtil.getStream("D:\\jpom\\agent\\data\\temp\\系统管理员\\sdfasdf\\example.com.csr");
        getSubjectDN(inputStream);
    }
}
