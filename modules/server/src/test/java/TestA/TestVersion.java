/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package TestA;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import org.junit.Test;

/**
 * @author bwcx_jzy
 * @since 2019/8/28
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
