package TestA;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import org.junit.Test;

/**
 * @author bwcx_jzy
 * @date 2019/8/28
 */
public class TestVersion {

	public static void main(String[] args) {
		String version = TestVersion.class.getPackage().getImplementationVersion();
		System.out.println(version);
	}

	@Test
	public void test1() {
		HttpRequest request = HttpUtil.createGet("https://gitee.com/dromara/Jpom/raw/master/CHANGELOG.md");
		String body = request.execute().body();
		System.out.println(body);
	}

	@Test
	public void test2() {
		System.out.println(CharsetUtil.defaultCharset());
	}
}
