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

import java.security.NoSuchAlgorithmException;

/**
 * @author loyal.f
 * @since 2023/3/9
 */
public class EncryptFactory {

    public static Encryptor createEncryptor(Integer type) throws NoSuchAlgorithmException {
        switch (type) {
            case 0:
                return NotEncryptor.getInstance();
            case 1:
                return BASE64Encryptor.getInstance();
            case 2:
                return AESEncryptor.getInstance();
            default:
                throw new NoSuchAlgorithmException("Unsupported encrypt type");
        }
    }
}
