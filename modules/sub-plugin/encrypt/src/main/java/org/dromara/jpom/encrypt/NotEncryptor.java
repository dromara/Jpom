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

/**
 * @author loyal.f
 * @since 2023/3/9
 */
public class NotEncryptor implements Encryptor {

    private static volatile NotEncryptor singleton;

    private NotEncryptor() {
        //构造器私有化，防止new，导致多个实例
    }

    public static Encryptor getInstance() {
        //向外暴露一个静态的公共方法  getInstance
        //第一层检查
        if (singleton == null) {
            //同步代码块
            synchronized (NotEncryptor.class) {
                //第二层检查
                if (singleton == null) {
                    singleton = new NotEncryptor();
                }
            }
        }
        return singleton;
    }

    @Override
    public String name() {
        return "no";
    }

    @Override
    public String encrypt(String input) {
        return input;
    }

    @Override
    public String decrypt(String input) throws Exception {
        return input;
    }
}
