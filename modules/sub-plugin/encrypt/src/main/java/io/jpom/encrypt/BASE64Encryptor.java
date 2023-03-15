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
