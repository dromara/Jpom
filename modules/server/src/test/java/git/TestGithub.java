package git;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import org.junit.Test;

/**
 * @author bwcx_jzy
 * @since 2021/12/19
 */
public class TestGithub {

	@Test
	public void testUser(){
		HttpRequest request = HttpUtil.createGet("https://api.github.com/user");

		request.header("Authorization","token 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");
		request.header("Accept","application/vnd.github.v3+json");
		String body = request.execute().body();
		System.out.println(body);
	}

	@Test
	public void test(){
		HttpRequest post = HttpUtil.createPost("https://api.github.com/repositories?since=824");
		String body = post.execute().body();
		System.out.println(body);
	}
}
