import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.crypto.PemUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;

import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Created by jiangzeyin on 2019/3/7.
 */
public class TestCert {
    public static void main(String[] args) {
//        HttpRequest request = HttpUtil.createPost("https://myssl.com/api/v1/tools/cert_decode");
//        request.form("certfile", new File("D:\\SystemDocument\\Desktop\\web_hulianwangjia\\full_chain.pem"));
//        request.form("type", "upload");
//        HttpResponse response = request.execute();
//        System.out.println(response.body());
//        D:\SystemDocument\Desktop

        PrivateKey privateKey = PemUtil.readPemPrivateKey(ResourceUtil.getStream("D:\\SystemDocument\\Desktop\\1979263_jpom.keepbx.cn.key"));
        PublicKey publicKey = PemUtil.readPemPublicKey(ResourceUtil.getStream("D:\\SystemDocument\\Desktop\\1979263_jpom.keepbx.cn.pem"));

        RSA rsa = new RSA(privateKey, publicKey);
        String str = "你好，Hutool";//测试字符串

        String encryptStr = rsa.encryptBase64(str, KeyType.PublicKey);
        String decryptStr = rsa.decryptStr(encryptStr, KeyType.PrivateKey);
        System.out.println(encryptStr);
        System.out.println(decryptStr);
        System.out.println(str.equals(decryptStr));
    }
}
