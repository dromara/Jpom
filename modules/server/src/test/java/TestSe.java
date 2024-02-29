/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
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
