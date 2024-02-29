/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
/**
 * Created by bwcx_jzy on 2018/9/30.
 */
public class TryFinally {
    public static void main(String[] args) {
        for (int i = 0; i < 10; i++) {
            try {
                if (i == 0) {
                    System.out.println("continue");
                    continue;
                }
            } finally {
                System.out.println(i + "finally");
            }

        }
    }
}
