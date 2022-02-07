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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ListContainersCmd;
import com.github.dockerjava.api.model.Container;
import io.jpom.plugin.IDefaultPlugin;
import io.jpom.plugin.PluginConfig;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * docker 插件
 *
 * @author bwcx_jzy
 * @since 2022/1/26
 */
@PluginConfig(name = "docker-cli")
public class DefaultDockerPluginImpl implements IDefaultPlugin {

	@Override
	public Object execute(Object main, Map<String, Object> parameter) throws Exception {
		String type = main.toString();
		switch (type) {
			case "build":
				new DockerBuild(parameter).build();
				return null;
			case "listContainer":
				return this.listContainerCmd(parameter);
			case "stopContainer":
				this.stopContainerCmd(parameter);
				return null;
			case "removeContainer":
				this.removeContainerCmd(parameter);
				return null;
			case "startContainer":
				this.startContainerCmd(parameter);
				return null;
			case "restartContainer":
				this.restartContainerCmd(parameter);
				return null;
			default:
				throw new IllegalArgumentException("不支持的类型");
		}
	}

	private List<JSONObject> listContainerCmd(Map<String, Object> parameter) {
		DockerClient dockerClient = DockerUtil.build(parameter);
		ListContainersCmd listContainersCmd = dockerClient.listContainersCmd();
		listContainersCmd.withShowAll(Convert.toBool(parameter.get("showAll"), true));
		String name = (String) parameter.get("name");
		if (StrUtil.isNotEmpty(name)) {
			listContainersCmd.withNameFilter(CollUtil.newArrayList(name));
		}
		String containerId = (String) parameter.get("containerId");
		if (StrUtil.isNotEmpty(containerId)) {
			listContainersCmd.withIdFilter(CollUtil.newArrayList(containerId));
		}
		List<Container> exec = listContainersCmd.exec();
		return exec.stream().map(container -> (JSONObject) JSONObject.toJSON(container)).collect(Collectors.toList());
	}

	private void restartContainerCmd(Map<String, Object> parameter) {
		String containerId = (String) parameter.get("containerId");
		DockerClient dockerClient = DockerUtil.build(parameter);
		dockerClient.restartContainerCmd(containerId).exec();
	}

	private void startContainerCmd(Map<String, Object> parameter) {
		String containerId = (String) parameter.get("containerId");
		DockerClient dockerClient = DockerUtil.build(parameter);
		dockerClient.startContainerCmd(containerId).exec();
	}

	private void stopContainerCmd(Map<String, Object> parameter) {
		String containerId = (String) parameter.get("containerId");
		DockerClient dockerClient = DockerUtil.build(parameter);
		dockerClient.stopContainerCmd(containerId).exec();
	}

	/**
	 * 删除容器
	 *
	 * @param parameter 参数
	 */
	private void removeContainerCmd(Map<String, Object> parameter) {
		String containerId = (String) parameter.get("containerId");
		DockerClient dockerClient = DockerUtil.build(parameter);
		DockerClientUtil.removeContainerCmd(dockerClient, containerId);
	}

}
