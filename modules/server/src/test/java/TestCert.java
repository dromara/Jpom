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
        String str = "您好，Hutool";//测试字符串

        String encryptStr = rsa.encryptBase64(str, KeyType.PublicKey);
        String decryptStr = rsa.decryptStr(encryptStr, KeyType.PrivateKey);
        System.out.println(encryptStr);
        System.out.println(decryptStr);
        System.out.println(str.equals(decryptStr));
    }
}
