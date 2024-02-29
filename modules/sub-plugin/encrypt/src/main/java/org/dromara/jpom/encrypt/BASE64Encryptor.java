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

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.CharsetUtil;

/**
 * base64
 *
 * @author loyal.f
 * @since 2023/3/9
 */
public class BASE64Encryptor implements Encryptor {

    private static volatile BASE64Encryptor singleton;

    private BASE64Encryptor() {
        //构造器私有化，防止new，导致多个实例
    }

    public static Encryptor getInstance() {
        //向外暴露一个静态的公共方法  getInstance
        //第一层检查
        if (singleton == null) {
            //同步代码块
            synchronized (BASE64Encryptor.class) {
                //第二层检查
                if (singleton == null) {
                    singleton = new BASE64Encryptor();
                }
            }
        }
        return singleton;
    }

    @Override
    public String name() {
        return "base64";
    }

    @Override
    public String encrypt(String input) {
        if (input == null) {
            return null;
        }
        return Base64.encode(input, CharsetUtil.CHARSET_UTF_8);
    }

    @Override
    public String decrypt(String input) {
        if (input == null) {
            return null;
        }
        return Base64.decodeStr(input);
    }
}
