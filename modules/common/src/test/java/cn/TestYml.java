/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package cn;

import cn.hutool.core.io.resource.ResourceUtil;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

import java.net.URL;

/**
 * @author bwcx_jzy
 * @since 2022/1/12
 */
public class TestYml {

	@Test
	public void test() {
		URL resource = ResourceUtil.getResource("test.yml");
		final Yaml yaml = new Yaml();
		Object load = yaml.load(ResourceUtil.getStream("test.yml"));
		System.out.println(load);

//		Dict dict = YamlUtil.loadByPath(resource.getPath());
//		System.out.println(dict);
	}
}
