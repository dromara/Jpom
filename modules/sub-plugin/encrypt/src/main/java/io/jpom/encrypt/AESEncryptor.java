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
package io.jpom.encrypt;

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
