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
package io.jpom;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.ServerOpenApi;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * @author bwcx_jzy
 * @since 2021/12/14
 */
public class BuildTriggerTest {

	@Test
	public void test() {
		// 8cf594526db74f0eb79cac6da141c655/219a4009a0a68173d8d643d237f2ca8ad797d41dc5bcfceb83da4f4f1d1dbe933a1
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("id", "8cf594526db74f0eb79cac6da141c655");
		jsonObject.put("token", "219a4009a0a68173d8d643d237f2ca8ad797d41dc5bcfceb83da4f4f1d1dbe933a1");
		//
		JSONArray jsonArray = new JSONArray();
		jsonArray.add(jsonObject);
		//
		HttpRequest post = HttpUtil.createPost("http://127.0.0.1:2122/" + ServerOpenApi.BUILD_TRIGGER_BUILD_BATCH);
		post.body(jsonArray.toString(), MediaType.APPLICATION_JSON_VALUE);
		String body = post.execute().body();
		System.out.println(body);
	}
}
