/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
import cn.hutool.http.HttpUtil;

/**
 * @author bwcx_jzy
 * @since 2019/7/15
 */
public class TestLogin {
    public static void main(String[] args) {
        while (true) {
            String r = HttpUtil.createPost("http://127.0.0.1:2122/userLogin").
                    form("userName", "admin").
                    form("userPwd", "5b67127803e84539ea43ce62657eca38a0903e93").execute().body();
            System.out.println(r);
        }
    }
}
