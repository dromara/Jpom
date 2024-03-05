/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.encrypt;

/**
 * @author loyal.f
 * @since 2023/3/9
 */
public interface Encryptor {


    /**
     * 加密方法
     *
     * @return 名称
     */
    String name();

    /**
     * 加密
     *
     * @param input 传入的测试
     * @return 加密后的字符串
     * @throws Exception 异常
     */
    String encrypt(String input) throws Exception;

    /**
     * 解密
     *
     * @param input 要解密的密文
     * @return 解密后的明文
     * @throws Exception 异常
     */
    String decrypt(String input) throws Exception;

}
