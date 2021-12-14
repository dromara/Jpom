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
import cn.hutool.core.io.resource.ResourceUtil;
import io.jpom.system.ConfigBean;
import org.junit.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileUrlResource;

import java.io.IOException;
import java.net.URL;
import java.util.List;

/**
 * @author bwcx_jzy
 * @date 2021/8/1
 */
public class TestYml {

	@Test
	public void test() throws IOException {
		String path = "D:\\Idea\\Jpom\\modules\\agent\\src\\main\\resources\\bin\\extConfig.yml";
		YamlPropertySourceLoader yamlPropertySourceLoader = new YamlPropertySourceLoader();
//		ByteArrayResource resource = new ByteArrayResource(StringUtil.deleteComment(content).getBytes(StandardCharsets.UTF_8));
		URL resource = ResourceUtil.getResource(path);
		List<PropertySource<?>> test = yamlPropertySourceLoader.load("test", new FileUrlResource(path));
		PropertySource<?> propertySource = test.get(0);
		System.out.println(propertySource);

		Object user = propertySource.getProperty(ConfigBean.AUTHORIZE_USER_KEY);
		System.out.println(user);
	}
}
