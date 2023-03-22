package cert;/*
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

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.crypto.GlobalBouncyCastleProvider;
import cn.hutool.crypto.KeyUtil;
import cn.hutool.crypto.PemUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import com.sun.security.cert.internal.x509.X509V1CertImpl;
import org.junit.Test;

import javax.security.cert.X509Certificate;
import java.io.File;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.Date;
import java.util.Enumeration;

/**
 * Created by jiangzeyin on 2019/3/7.
 */
public class TestCert {

    static {
        //Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        GlobalBouncyCastleProvider.setUseBouncyCastle(false);
    }

    @Test
    public void testPfx() throws Exception {
        File file = FileUtil.file("D:\\System-Data\\Downloads\\download.jpom.top\\8215371_download.jpom.top_tomcat", "8215371_download.jpom.top.pfx");
        char[] password = "skkervmb".toCharArray();
        KeyStore keyStore = KeyUtil.readPKCS12KeyStore(file, password);
        String type = keyStore.getType();
//        keyStore.load(FileUtil.getInputStream(file), password);
        Enumeration<String> aliases = keyStore.aliases();
        if (aliases.hasMoreElements())// we are readin just one certificate.
        {
            String keyAlias = aliases.nextElement();
            System.out.println("alias=[" + keyAlias + "]");
            Certificate certificate = keyStore.getCertificate(keyAlias);
            System.out.println(certificate.getType());
            PrivateKey prikey = (PrivateKey) keyStore.getKey(keyAlias, password);
            {

                PublicKey pubkey = certificate.getPublicKey();
            }

            {
                X509Certificate cert = X509Certificate.getInstance(certificate.getEncoded());
                byte[] encoded = cert.getEncoded();
                System.out.println("指纹：" + SecureUtil.sha1().digestHex(encoded));
                Date notBefore = cert.getNotBefore();
                Date notAfter = cert.getNotAfter();
                BigInteger serialNumber = cert.getSerialNumber();
                if (cert instanceof X509V1CertImpl) {
                    X509V1CertImpl x509V1Cert = (X509V1CertImpl) cert;
                    java.security.cert.X509Certificate x509Certificate = x509V1Cert.getX509Certificate();
                    //AuthorityKeyIdentifier
                    byte[] extensionValue1 = x509Certificate.getExtensionValue("2.5.29.35");
                    // SubjectKeyIdentifier
                    byte[] extensionValue2 = x509Certificate.getExtensionValue("2.5.29.14");
                    System.out.println(HexUtil.encodeHexStr(ArrayUtil.sub(extensionValue1, 6, extensionValue1.length)));
                    System.out.println(HexUtil.encodeHexStr(ArrayUtil.sub(extensionValue2, 4, extensionValue2.length)));
                }
                System.out.println(serialNumber.toString(16));
                int version = cert.getVersion();
                Principal subjectDN = cert.getSubjectDN();
                Principal issuerDN = cert.getIssuerDN();
                String issuerDNName = issuerDN.getName();
                String subjectDNName = subjectDN.getName();
                String sigAlgOID = cert.getSigAlgOID();
                String sigAlgName = cert.getSigAlgName();
                System.out.println(serialNumber);
                System.out.println(notBefore + "  " + notAfter);
                System.out.println(sigAlgName);
                System.out.println(sigAlgOID);
                System.out.println(issuerDNName);
                System.out.println(subjectDNName);
            }
        }
//        KeyStore keyStore = KeyUtil.readJKSKeyStore(file, "skkervmb".toCharArray());
        System.out.println(keyStore);
    }

    @Test
    public void testKey() throws Exception {
        File file = FileUtil.file("D:\\System-Data\\Downloads\\download.jpom.top\\8215371_download.jpom.top_nginx", "8215371_download.jpom.top.pem");

        Certificate certificate = KeyUtil.readX509Certificate(FileUtil.getInputStream(file));
//        KeyStore keyStore = KeyUtil.readJKSKeyStore(file, "skkervmb".toCharArray());
        {
            X509Certificate cert = X509Certificate.getInstance(certificate.getEncoded());
            byte[] encoded = cert.getEncoded();
            System.out.println("指纹：" + SecureUtil.sha256().digestHex(encoded));
            Date notBefore = cert.getNotBefore();
            Date notAfter = cert.getNotAfter();
            BigInteger serialNumber = cert.getSerialNumber();
            System.out.println(certificate.getType());
            System.out.println(serialNumber);
            System.out.println(notBefore + "  " + notAfter);
        }
    }

    @Test
    public void testJks() throws Exception {
        File file = FileUtil.file("D:\\System-Data\\Downloads\\download.jpom.top\\8215371_download.jpom.top_jks", "8215371_download.jpom.top.jks");
        char[] password = "hibuvbsp".toCharArray();
        KeyStore keyStore = KeyUtil.readJKSKeyStore(file, password);
        keyStore.load(FileUtil.getInputStream(file), password);
        Enumeration<String> aliases = keyStore.aliases();
        if (aliases.hasMoreElements())// we are readin just one certificate.
        {
            String keyAlias = aliases.nextElement();
            System.out.println("alias=[" + keyAlias + "]");
            Certificate certificate = keyStore.getCertificate(keyAlias);
            PrivateKey prikey = (PrivateKey) keyStore.getKey(keyAlias, password);
            {

                PublicKey pubkey = certificate.getPublicKey();
            }
            {
                X509Certificate cert = X509Certificate.getInstance(certificate.getEncoded());
                byte[] encoded = cert.getEncoded();
                System.out.println("指纹：" + SecureUtil.sha256().digestHex(encoded));
                Date notBefore = cert.getNotBefore();
                Date notAfter = cert.getNotAfter();
                BigInteger serialNumber = cert.getSerialNumber();
                System.out.println(serialNumber);
                System.out.println(notBefore + "  " + notAfter);
            }
        }
//        KeyStore keyStore = KeyUtil.readJKSKeyStore(file, "skkervmb".toCharArray());
        System.out.println(keyStore);
    }

    @Test
    public void testCrt() throws Exception {
        PrivateKey privateKey = PemUtil.readPemPrivateKey(FileUtil.getInputStream("D:\\System-Data\\Downloads\\download.jpom.top\\8215371_download.jpom.top_apache\\8215371_download.jpom.top.key"));
        X509Certificate publicCert;
        PublicKey publicKey1;
        Certificate certificate1;

        {
            File file = FileUtil.file("D:\\System-Data\\Downloads\\download.jpom.top\\8215371_download.jpom.top_apache", "8215371_download.jpom.top_public.crt");
            certificate1 = KeyUtil.readX509Certificate(FileUtil.getInputStream(file));
            String certificate1Type = certificate1.getType();

            System.out.println(certificate1Type);
            {
                publicCert = X509Certificate.getInstance(certificate1.getEncoded());
                byte[] encoded = publicCert.getEncoded();
                System.out.println("指纹：" + SecureUtil.sha256().digestHex(encoded));
                System.out.println(publicCert.getIssuerDN().equals(publicCert.getSubjectDN()) + "  " + publicCert.getIssuerDN() + "  " + publicCert.getSubjectDN());
                Date notBefore = publicCert.getNotBefore();
                Date notAfter = publicCert.getNotAfter();
                BigInteger serialNumber = publicCert.getSerialNumber();
                System.out.println(serialNumber);
                System.out.println(notBefore + "  " + notAfter);
            }

            publicKey1 = certificate1.getPublicKey();
            RSA rsa = new RSA(privateKey, publicKey1);
            String str = "您好，Hutool";//测试字符串

            String encryptStr = rsa.encryptBase64(str, KeyType.PublicKey);
            String decryptStr = rsa.decryptStr(encryptStr, KeyType.PrivateKey);
            System.out.println(str.equals(decryptStr));

        }

        X509Certificate chainCert;
        {
            File file = FileUtil.file("D:\\System-Data\\Downloads\\download.jpom.top\\8215371_download.jpom.top_apache", "8215371_download.jpom.top_chain.crt");
            Certificate certificate = KeyUtil.readX509Certificate(FileUtil.getInputStream(file));
            String certificate2Type = certificate.getType();
            System.out.println(certificate2Type);
            {
                chainCert = X509Certificate.getInstance(certificate.getEncoded());
                System.out.println(chainCert.getIssuerDN().equals(chainCert.getSubjectDN()) + "  " + chainCert.getIssuerDN() + "  " + chainCert.getSubjectDN());
                Date notBefore = chainCert.getNotBefore();
                Date notAfter = chainCert.getNotAfter();
                BigInteger serialNumber = chainCert.getSerialNumber();
                System.out.println(serialNumber);
                System.out.println(notBefore + "  " + notAfter);
            }
            System.out.println(certificate.equals(certificate1));
            PublicKey publicKey = certificate.getPublicKey();
            System.out.println(publicKey.equals(publicKey1));
            System.out.println(certificate1.getEncoded().equals(certificate.getEncoded()));
//            RSA rsa = new RSA(privateKey, publicKey);
//            String str = "您好，Hutool";//测试字符串
//
//            String encryptStr = rsa.encryptBase64(str, KeyType.PublicKey);
//            String decryptStr = rsa.decryptStr(encryptStr, KeyType.PrivateKey);
//            System.out.println(str.equals(decryptStr));
        }
//        X509Certificate[] chain = new X509Certificate[]{publicCert, cert2};
        // Verify certificate chain
        publicCert.checkValidity();
        chainCert.checkValidity();

        publicCert.verify(chainCert.getPublicKey());
    }


    @Test
    public void testCa() throws Exception {
        File ca = FileUtil.file("D:\\System-Data\\Desktop\\ca", "ca.pem");
        File crt = FileUtil.file("D:\\System-Data\\Desktop\\ca", "cert.pem");
        File key = FileUtil.file("D:\\System-Data\\Desktop\\ca", "key.pem");

        PrivateKey privateKey = PemUtil.readPemPrivateKey(FileUtil.getInputStream(key));
        X509Certificate publicCert;
        PublicKey publicKey1;
        Certificate certificate1;

        {

            certificate1 = KeyUtil.readX509Certificate(FileUtil.getInputStream(crt));
            {
                publicCert = X509Certificate.getInstance(certificate1.getEncoded());
                byte[] encoded = publicCert.getEncoded();
                System.out.println("指纹：" + SecureUtil.sha256().digestHex(encoded));
                System.out.println(publicCert.getIssuerDN().equals(publicCert.getSubjectDN()) + "  " + publicCert.getIssuerDN() + "  " + publicCert.getSubjectDN());
                Date notBefore = publicCert.getNotBefore();
                Date notAfter = publicCert.getNotAfter();
                BigInteger serialNumber = publicCert.getSerialNumber();
                System.out.println(serialNumber);
                System.out.println(notBefore + "  " + notAfter);
            }

            publicKey1 = certificate1.getPublicKey();
            RSA rsa = new RSA(privateKey, publicKey1);
            String str = "您好，Hutool";//测试字符串

            String encryptStr = rsa.encryptBase64(str, KeyType.PublicKey);
            String decryptStr = rsa.decryptStr(encryptStr, KeyType.PrivateKey);
            System.out.println(str.equals(decryptStr));

        }

        X509Certificate chainCert;
        {

            Certificate certificate = KeyUtil.readX509Certificate(FileUtil.getInputStream(ca));
            {
                chainCert = X509Certificate.getInstance(certificate.getEncoded());
                System.out.println(chainCert.getIssuerDN().equals(chainCert.getSubjectDN()) + "  " + chainCert.getIssuerDN() + "  " + chainCert.getSubjectDN());
                Date notBefore = chainCert.getNotBefore();
                Date notAfter = chainCert.getNotAfter();
                BigInteger serialNumber = chainCert.getSerialNumber();
                System.out.println(chainCert.getSubjectDN().getName());
                System.out.println(serialNumber);
                System.out.println(notBefore + "  " + notAfter);
            }
            System.out.println(certificate.equals(certificate1));
            PublicKey publicKey = certificate.getPublicKey();
            System.out.println(publicKey.equals(publicKey1));
            System.out.println(certificate1.getEncoded().equals(certificate.getEncoded()));
//            RSA rsa = new RSA(privateKey, publicKey);
//            String str = "您好，Hutool";//测试字符串
//
//            String encryptStr = rsa.encryptBase64(str, KeyType.PublicKey);
//            String decryptStr = rsa.decryptStr(encryptStr, KeyType.PrivateKey);
//            System.out.println(str.equals(decryptStr));
        }
//        X509Certificate[] chain = new X509Certificate[]{publicCert, cert2};
        // Verify certificate chain
        publicCert.checkValidity();
        chainCert.checkValidity();

        publicCert.verify(chainCert.getPublicKey());
    }

    public void main(String[] args) {
//        HttpRequest request = HttpUtil.createPost("https://myssl.com/api/v1/tools/cert_decode");
//        request.form("certfile", new File("D:\\SystemDocument\\Desktop\\web_hulianwangjia\\full_chain.pem"));
//        request.form("type", "upload");
//        HttpResponse response = request.execute();
//        System.out.println(response.body());
//        D:\SystemDocument\Desktop

        PrivateKey privateKey = PemUtil.readPemPrivateKey(ResourceUtil.getStream("D:\\SystemDocument\\Desktop\\1979263_jpom.keepbx.cn.key"));
        PublicKey publicKey = PemUtil.readPemPublicKey(ResourceUtil.getStream("D:\\SystemDocument\\Desktop\\1979263_jpom.keepbx.cn.pem"));

        RSA rsa = new RSA(privateKey, publicKey);
        String str = "您好，Hutool";//测试字符串

        String encryptStr = rsa.encryptBase64(str, KeyType.PublicKey);
        String decryptStr = rsa.decryptStr(encryptStr, KeyType.PrivateKey);
        System.out.println(encryptStr);
        System.out.println(decryptStr);
        System.out.println(str.equals(decryptStr));
    }
}
