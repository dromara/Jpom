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

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import cn.hutool.crypto.symmetric.DES;
import org.junit.jupiter.api.Test;

/**
 * @author bwcx_jzy
 * @since 2022/7/22
 */
public class TestSe {

    @Test
    public void test() {
        DES des = SecureUtil.des("KZQfFBJTW2v6obS1".getBytes());
        String uuid = IdUtil.fastSimpleUUID();
        String data = uuid + ":ad7de8fec89d48d6b12d1eeace1543ad:ad7de8fec89d4";
        String sss = des.encryptHex(data);
        System.out.println(sss);
        System.out.println(StrUtil.length(uuid) + " " + StrUtil.length(data) + " " + StrUtil.length(sss));
        String decryptStr = des.decryptStr(sss);
        System.out.println(decryptStr);
        //
        AES aes = SecureUtil.aes("KZQfFBJTW2v6obS1".getBytes());
        sss = aes.encryptHex("ad7de8fec89d48d6b12d1eeace1543ad:ad7de8fec89d48d6b12d1eeace1543ad");
        System.out.println(sss);
        System.out.println(StrUtil.length(sss));
        decryptStr = aes.decryptStr(sss);
        System.out.println(decryptStr);

        //
    }
}
