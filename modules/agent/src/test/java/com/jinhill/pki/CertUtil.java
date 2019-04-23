package com.jinhill.pki;

import cn.hutool.core.io.resource.ResourceUtil;

import java.io.InputStream;
import java.security.Security;
import java.security.cert.CertificateException;
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
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dn;
    }

    public static String parseCertDN(String dn, String type) {
        type = type + "=";
        String[] split = dn.split(",");
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
        // TODO Auto-generated method stub
        InputStream inputStream = ResourceUtil.getStream("D:\\jpom\\agent\\data\\temp\\系统管理员\\sdfasdf\\example.com.csr");
        getSubjectDN(inputStream);
    }
}
