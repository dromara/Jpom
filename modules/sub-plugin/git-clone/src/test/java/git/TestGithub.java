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
