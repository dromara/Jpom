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
