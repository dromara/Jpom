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
//import cn.hutool.core.io.FileUtil;
//import org.apache.commons.codec.binary.Base64;
//import sun.misc.BASE64Decoder;
//import sun.misc.BASE64Encoder;
//
//import java.io.BufferedInputStream;
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileReader;
//import java.security.KeyFactory;
//import java.security.PrivateKey;
//import java.security.Signature;
//import java.security.cert.Certificate;
//import java.security.cert.CertificateFactory;
//import java.security.cert.X509Certificate;
//import java.security.spec.PKCS8EncodedKeySpec;
//
//public class DTest {
//
//
//    public static void main(String[] args) throws Exception {
//
//    }
//
//    private static void printTrack() {
//        StackTraceElement[] st = Thread.currentThread().getStackTrace();
//        StringBuilder sbf = new StringBuilder();
//        for (StackTraceElement e : st) {
//            if (sbf.length() > 0) {
//                sbf.append(" <- ");
//                sbf.append(System.getProperty("line.separator"));
//            }
//            sbf.append(java.text.MessageFormat.format("{0}.{1}() {2}"
//                    , e.getClassName()
//                    , e.getMethodName()
//                    , e.getLineNumber()));
//        }
//        System.out.println(sbf.toString());
//    }
//
//
//    private static void cert() throws Exception {
//        String plain = "aaaa";
//        File certFile = FileUtil.file("G:/soft/nginx/cert/full_chain.pem");
//        CertificateFactory cf = CertificateFactory.getInstance("X.509");
//        BufferedInputStream inStream = FileUtil.getInputStream(certFile);
//        // 创建证书对象
//        Certificate certificate = cf.generateCertificate(inStream);
//        inStream.close();
//
//        String sigAlgName = ((X509Certificate) certificate).getSigAlgName();
//
//        Signature instance = Signature.getInstance(sigAlgName);
//        PrivateKey privateKey = getPrivateKey(new File("G:/soft/nginx/cert/private.key"));
//        instance.initSign(privateKey);
//        instance.update(plain.getBytes());
//        byte[] signed = instance.sign();
//        BASE64Encoder encoder = new BASE64Encoder();
//        //签名
//        String encode = encoder.encode(signed);
//
//        Signature signature = Signature.getInstance(sigAlgName);
//        signature.initVerify(certificate.getPublicKey());
//        signature.update(plain.getBytes());
//        BASE64Decoder decoder = new BASE64Decoder();
//        boolean verify = signature.verify(decoder.decodeBuffer(encode));
//        System.out.println(verify);
//    }
//
//    /**
//     * 利用java自带的方法读取openssl私钥,openssl私钥文件格式为pem，需要去除页眉页脚后，再进行base64位解码才能被java读取
//     * 注意该方法有缺陷,只是简单的根据注释将页眉页脚去掉了,不是很完善,如果页眉页脚前面有空格和注释的情况的会有问题,保留此方法是为方便弄清楚openssl私钥解析原理
//     */
//    private static PrivateKey getPrivateKey(File file) {
//        if (file == null) {
//            return null;
//        }
//        PrivateKey privKey;
//        try {
//            BufferedReader privateKey = new BufferedReader(new FileReader(
//                    file));
//            String line;
//            StringBuilder strPrivateKey = new StringBuilder();
//            while ((line = privateKey.readLine()) != null) {
//                if (line.contains("--")) {//过滤掉首尾页眉页脚
//                    continue;
//                }
//                strPrivateKey.append(line);
//            }
//            privateKey.close();
//            //使用base64位解码
//            byte[] privKeyByte = Base64.decodeBase64(strPrivateKey.toString());
//            //私钥需要使用pkcs8格式编码
//            PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(privKeyByte);
//            KeyFactory kf = KeyFactory.getInstance("RSA");
//            privKey = kf.generatePrivate(privKeySpec);
//            return privKey;
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return null;
//    }
//}
