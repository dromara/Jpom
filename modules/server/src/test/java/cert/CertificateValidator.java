/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
//package cert;
//
//import cn.hutool.core.io.FileUtil;
//import cn.hutool.core.util.CharsetUtil;
//import cn.hutool.crypto.PemUtil;
//import org.bouncycastle.jce.provider.BouncyCastleProvider;
//import org.bouncycastle.util.io.pem.PemObject;
//import org.bouncycastle.util.io.pem.PemReader;
//
//import java.io.FileInputStream;
//import java.security.*;
//import java.security.cert.CertificateFactory;
//import java.security.cert.X509Certificate;
//import java.security.spec.PKCS8EncodedKeySpec;
//
///**
// * @author bwcx_jzy
// * @since 2023/3/22
// */
//
//public class CertificateValidator {
//    public static void main(String[] args) throws Exception {
//
//        // Add Bouncy Castle provider
//        Security.addProvider(new BouncyCastleProvider());
//
//        // Load public certificate and certificate chain
//        CertificateFactory certFactory = CertificateFactory.getInstance("X.509", "BC");
//        X509Certificate publicCert = (X509Certificate) certFactory.generateCertificate(new FileInputStream("public.crt"));
//        X509Certificate[] chain = {publicCert};
//        PemUtil.readPemObject("");
//        PemReader pemReader = new PemReader(new FileInputStream("chain.crt"));
//        while (true) {
//            PemObject pem = pemReader.readPemObject();
//            if (pem == null) break;
//            X509Certificate cert = (X509Certificate) certFactory.generateCertificate(pem.getContent());
//            chain = appendToArray(chain, cert);
//        }
//        pemReader.close();
//
//        // Load private key
//        PemReader keyReader = new PemReader(FileUtil.getReader(FileUtil.file("D:\\System-Data\\Downloads\\download.jpom.top\\8215371_download.jpom.top_apache\\8215371_download.jpom.top.key"), CharsetUtil.CHARSET_UTF_8));
//        byte[] keyBytes = keyReader.readPemObject().getContent();
//        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
//        KeyFactory keyFactory = KeyFactory.getInstance("RSA", "BC");
//        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
//        keyReader.close();
//
//        // Verify certificate chain
//        publicCert.checkValidity();
//        for (int i = 1; i < chain.length; i++) {
//            chain[i].checkValidity();
//            chain[i].verify(chain[i - 1].getPublicKey());
//        }
//
//        // Verify private key
//        PublicKey publicKey = publicCert.getPublicKey();
//        byte[] message = "test message".getBytes();
//        Signature signature = Signature.getInstance("SHA256withRSA", "BC");
//        signature.initSign(privateKey);
//        signature.update(message);
//        byte[] signatureBytes = signature.sign();
//        signature.initVerify(publicKey);
//        signature.update(message);
//        boolean verified = signature.verify(signatureBytes);
//
//        // Print result
//        System.out.println("Certificate and private key " + (verified ? "match" : "do not match"));
//    }
//
//    private static <T> T[] appendToArray(T[] array, T item) {
//        T[] newArray = (T[]) java.lang.reflect.Array.newInstance(array.getClass().getComponentType(), array.length + 1);
//        System.arraycopy(array, 0, newArray, 0, array.length);
//        newArray[newArray.length - 1] = item;
//        return newArray;
//    }
//}
