/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.encrypt;

import cn.hutool.core.util.SystemPropsUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;

/**
 * aes
 *
 * @author loyal.f
 * @since 2023/3/9
 */
public class AESEncryptor implements Encryptor {

    private final byte[] keyByte;

    private static volatile AESEncryptor singleton;

    private AESEncryptor(String key) {
        //构造器私有化，防止new，导致多个实例
        this.keyByte = key.getBytes();
    }

    public static Encryptor getInstance() {
        //向外暴露一个静态的公共方法  getInstance
        //第一层检查
        if (singleton == null) {
            //同步代码块
            synchronized (AESEncryptor.class) {
                //第二层检查
                if (singleton == null) {
                    String aesKey = SystemPropsUtil.get("JPOM_ENCRYPT_AES_KEY", "Djnn3runZBzdv9Nv");
                    singleton = new AESEncryptor(aesKey);
                }
            }
        }
        return singleton;
    }


    @Override
    public String name() {
        return "aes";
    }

    @Override
    public String encrypt(String input) throws Exception {
        if (input == null) {
            return null;
        }
        AES aes = SecureUtil.aes(keyByte);
        return aes.encryptHex(input);
    }

    @Override
    public String decrypt(String input) throws Exception {
        if (input == null) {
            return null;
        }
        AES aes = SecureUtil.aes(keyByte);
        return aes.decryptStr(input);
    }

}
