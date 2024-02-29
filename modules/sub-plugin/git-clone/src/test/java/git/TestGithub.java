/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package git;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import org.junit.Test;

import java.util.function.Consumer;

/**
 * @author bwcx_jzy
 * @since 2021/12/19
 */
public class TestGithub {

    @Test
    public void testUser() {
        HttpRequest request = HttpUtil.createGet("https://api.github.com/user");

        request.header("Authorization", "token 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");
        request.header("Accept", "application/vnd.github.v3+json");
        request.then(new Consumer<HttpResponse>() {
            @Override
            public void accept(HttpResponse httpResponse) {
                String body = httpResponse.body();
                System.out.println(body);
            }
        });
        System.out.println("123");

    }

    @Test
    public void testGielab() {
        String value = "glpat-rkreLo8NkZShSVyhgWyq";
        HttpRequest request = HttpUtil.createGet("https://gitlab.com/api/v4/user");
        request.form("access_token", value);
        String body = request.execute().body();
        System.out.println(body);

        HttpRequest httpRequest = HttpUtil.createGet("https://gitlab.com/api/v4/projects");

        httpRequest.form("private_token", value);
        httpRequest.form("membership", true);
        httpRequest.form("simple", true);

        httpRequest.form("page", 1);
        httpRequest.form("per_page", "15");
        String body1 = httpRequest.execute().body();
        System.out.println(body1);
    }


    @Test
    public void test() {
        HttpRequest post = HttpUtil.createPost("https://api.github.com/repositories?since=824");
        String body = post.execute().body();
        System.out.println(body);
    }
}
