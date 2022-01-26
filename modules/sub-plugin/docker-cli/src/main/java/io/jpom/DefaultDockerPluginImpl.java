package io.jpom;

import io.jpom.plugin.IDefaultPlugin;
import io.jpom.plugin.PluginConfig;

import java.util.Map;

/**
 * @author bwcx_jzy
 * @since 2022/1/26
 */
@PluginConfig(name = "docker-cli")
public class DefaultDockerPluginImpl implements IDefaultPlugin {

	@Override
	public Object execute(Object main, Map<String, Object> parameter) throws Exception {
		return null;
	}
}
