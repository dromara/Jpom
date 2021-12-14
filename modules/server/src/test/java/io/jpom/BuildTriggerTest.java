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
