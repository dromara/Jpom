import ch.qos.logback.core.util.SystemInfo;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.text.StrSpliter;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.RuntimeInfo;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.keepbx.jpom.common.commander.AbstractCommander;
import org.apache.commons.codec.binary.Base64;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.management.MBeanServer;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.math.BigDecimal;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.List;

public class DTest {


    public static void main(String[] args) throws Exception {
//        long start = System.currentTimeMillis();
//        String s1 = execSystemCommand("netstat -antp |grep  32936 | head -1 ");
//        System.out.println(s1);
//        long l = System.currentTimeMillis();
//        System.out.println(l-start);

        String command = "tasklist /V /FI \"pid eq 3280\"";
        String result = AbstractCommander.getInstance().execCommand(command);
        List<String> list = StrSpliter.splitTrim(result, "\n", true);
        if (list.size() >= 3) {
            List<String> memList = StrSpliter.splitTrim(list.get(2), " ", true);
            System.out.println(memList.get(0));
            String mem = memList.get(4).replace(",", "");
            long aLong = Convert.toLong(mem, 0L);
            System.out.println(aLong);
            RuntimeInfo runtimeInfo = new RuntimeInfo();
            long totalMemory = runtimeInfo.getTotalMemory();
            double d = totalMemory / 1024D;
            System.out.println(d);
            double v = new BigDecimal(aLong).divide(new BigDecimal(d), 2, BigDecimal.ROUND_HALF_UP).doubleValue();
            System.out.println(v * 100 + "%");
            System.out.println(runtimeInfo.toString());
            System.out.println(memList.get(6));
            System.out.println(memList.get(8));
        }
    }

    private static void printTrack() {
        StackTraceElement[] st = Thread.currentThread().getStackTrace();
        if (st == null) {
            System.out.println("无堆栈...");
            return;
        }
        StringBuffer sbf = new StringBuffer();
        for (StackTraceElement e : st) {
            if (sbf.length() > 0) {
                sbf.append(" <- ");
                sbf.append(System.getProperty("line.separator"));
            }
            sbf.append(java.text.MessageFormat.format("{0}.{1}() {2}"
                    , e.getClassName()
                    , e.getMethodName()
                    , e.getLineNumber()));
        }
        System.out.println(sbf.toString());
    }

    private static String execSystemCommand(String command) {
        String result = "error";
        try {
            //执行linux系统命令
            String[] cmd = new String[]{"/bin/sh", "-c", command};
            result = exec(cmd);
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error("执行命令异常", e);
            result += e.getMessage();
        }
        return result;
    }

    private static String exec(String[] cmd) throws IOException, InterruptedException {
        String result;
        Process process;
        if (cmd.length == 1) {
            process = Runtime.getRuntime().exec(cmd[0]);
        } else {
            process = Runtime.getRuntime().exec(cmd);
        }
        InputStream is;
        int wait = process.waitFor();
        if (wait == 0) {
            is = process.getInputStream();
        } else {
            is = process.getErrorStream();
        }
        result = IoUtil.read(is, CharsetUtil.UTF_8);
        is.close();
        process.destroy();
        if (StrUtil.isEmpty(result) && wait != 0) {
            result = "没有返回任何执行信息";
        }
        return result;
    }

    private static void cert() throws Exception {
        String plain = "aaaa";
        File certFile = FileUtil.file("G:/soft/nginx/cert/full_chain.pem");
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        BufferedInputStream inStream = FileUtil.getInputStream(certFile);
        // 创建证书对象
        Certificate certificate = cf.generateCertificate(inStream);
        inStream.close();

        String sigAlgName = ((X509Certificate) certificate).getSigAlgName();

        Signature instance = Signature.getInstance(sigAlgName);
        PrivateKey privateKey = getPrivateKey(new File("G:/soft/nginx/cert/private.key"));
        instance.initSign(privateKey);
        instance.update(plain.getBytes());
        byte[] signed = instance.sign();
        BASE64Encoder encoder = new BASE64Encoder();
        //签名
        String encode = encoder.encode(signed);

        Signature signature = Signature.getInstance(sigAlgName);
        signature.initVerify(certificate.getPublicKey());
        signature.update(plain.getBytes());
        BASE64Decoder decoder = new BASE64Decoder();
        boolean verify = signature.verify(decoder.decodeBuffer(encode));
        System.out.println(verify);
    }

    /**
     * 利用java自带的方法读取openssl私钥,openssl私钥文件格式为pem，需要去除页眉页脚后，再进行base64位解码才能被java读取
     * 注意该方法有缺陷,只是简单的根据注释将页眉页脚去掉了,不是很完善,如果页眉页脚前面有空格和注释的情况的会有问题,保留此方法是为方便弄清楚openssl私钥解析原理
     */
    private static PrivateKey getPrivateKey(File file) {
        if (file == null) {
            return null;
        }
        PrivateKey privKey;
        try {
            BufferedReader privateKey = new BufferedReader(new FileReader(
                    file));
            String line;
            StringBuilder strPrivateKey = new StringBuilder();
            while ((line = privateKey.readLine()) != null) {
                if (line.contains("--")) {//过滤掉首尾页眉页脚
                    continue;
                }
                strPrivateKey.append(line);
            }
            privateKey.close();
            //使用base64位解码
            byte[] privKeyByte = Base64.decodeBase64(strPrivateKey.toString());
            //私钥需要使用pkcs8格式编码
            PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(privKeyByte);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            privKey = kf.generatePrivate(privKeySpec);
            return privKey;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
