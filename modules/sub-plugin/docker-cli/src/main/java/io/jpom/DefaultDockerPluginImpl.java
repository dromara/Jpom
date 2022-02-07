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
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Image;
import io.jpom.plugin.IDefaultPlugin;
import io.jpom.plugin.PluginConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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
				try (DockerBuild dockerBuild = new DockerBuild(parameter)) {
					dockerBuild.build();
				}
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
			case "listImages":
				return this.listImagesCmd(parameter);
			case "removeImage":
				this.removeImageCmd(parameter);
				return null;
			case "listVolumes":
				return this.listVolumesCmd(parameter);
			case "removeVolume":
				this.removeVolumeCmd(parameter);
				return null;
			default:
				throw new IllegalArgumentException("不支持的类型");
		}
	}

	private List<JSONObject> listVolumesCmd(Map<String, Object> parameter) {
		DockerClient dockerClient = DockerUtil.build(parameter);
		try {
			ListVolumesCmd listVolumesCmd = dockerClient.listVolumesCmd();
			Boolean dangling = Convert.toBool(parameter.get("dangling"), false);
			if (dangling) {
				listVolumesCmd.withDanglingFilter(dangling);
			}
			String name = (String) parameter.get("name");
			if (StrUtil.isNotEmpty(name)) {
				listVolumesCmd.withFilter("name", CollUtil.newArrayList(name));
			}

			ListVolumesResponse exec = listVolumesCmd.exec();
			List<InspectVolumeResponse> volumes = exec.getVolumes();
			volumes = ObjectUtil.defaultIfNull(volumes, new ArrayList<>());
			return volumes.stream().map((Function<InspectVolumeResponse, Object>) inspectVolumeResponse -> {
				InspectVolumeCmd inspectVolumeCmd = dockerClient.inspectVolumeCmd(inspectVolumeResponse.getName());
				return inspectVolumeCmd.exec();
			}).map(container -> (JSONObject) JSONObject.toJSON(container)).collect(Collectors.toList());
		} finally {
			IoUtil.close(dockerClient);
		}
	}

	private void removeVolumeCmd(Map<String, Object> parameter) {
		DockerClient dockerClient = DockerUtil.build(parameter);
		try {
			String volumeName = (String) parameter.get("volumeName");
			dockerClient.removeVolumeCmd(volumeName).exec();
		} finally {
			IoUtil.close(dockerClient);
		}
	}


	private List<JSONObject> listImagesCmd(Map<String, Object> parameter) {
		DockerClient dockerClient = DockerUtil.build(parameter);
		try {
			ListImagesCmd listImagesCmd = dockerClient.listImagesCmd();
			listImagesCmd.withShowAll(Convert.toBool(parameter.get("showAll"), true));
			listImagesCmd.withDanglingFilter(Convert.toBool(parameter.get("dangling"), false));

			String name = (String) parameter.get("name");
			if (StrUtil.isNotEmpty(name)) {
				listImagesCmd.withImageNameFilter(name);
			}
			List<Image> exec = listImagesCmd.exec();
			exec = ObjectUtil.defaultIfNull(exec, new ArrayList<>());
			return exec.stream().map(container -> (JSONObject) JSONObject.toJSON(container)).collect(Collectors.toList());
		} finally {
			IoUtil.close(dockerClient);
		}
	}

	private void removeImageCmd(Map<String, Object> parameter) {
		DockerClient dockerClient = DockerUtil.build(parameter);
		try {
			String imageId = (String) parameter.get("imageId");
			dockerClient.removeImageCmd(imageId).withForce(true).exec();
		} finally {
			IoUtil.close(dockerClient);
		}
	}


	private List<JSONObject> listContainerCmd(Map<String, Object> parameter) {
		DockerClient dockerClient = DockerUtil.build(parameter);
		try {
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
//		String imageId = (String) parameter.get("imageId");
//		if (StrUtil.isNotEmpty(imageId)) {
//			listContainersCmd.withFilter("image", CollUtil.newArrayList(imageId));
//		}
			List<Container> exec = listContainersCmd.exec();
			exec = ObjectUtil.defaultIfNull(exec, new ArrayList<>());
			return exec.stream().map(container -> (JSONObject) JSONObject.toJSON(container)).collect(Collectors.toList());
		} finally {
			IoUtil.close(dockerClient);
		}
	}

	private void restartContainerCmd(Map<String, Object> parameter) {
		String containerId = (String) parameter.get("containerId");
		DockerClient dockerClient = DockerUtil.build(parameter);
		try {
			dockerClient.restartContainerCmd(containerId).exec();
		} finally {
			IoUtil.close(dockerClient);
		}
	}

	private void startContainerCmd(Map<String, Object> parameter) {
		String containerId = (String) parameter.get("containerId");
		DockerClient dockerClient = DockerUtil.build(parameter);
		try {
			dockerClient.startContainerCmd(containerId).exec();
		} finally {
			IoUtil.close(dockerClient);
		}
	}

	private void stopContainerCmd(Map<String, Object> parameter) {
		String containerId = (String) parameter.get("containerId");
		DockerClient dockerClient = DockerUtil.build(parameter);
		try {
			dockerClient.stopContainerCmd(containerId).exec();
		} finally {
			IoUtil.close(dockerClient);
		}
	}

	/**
	 * 删除容器
	 *
	 * @param parameter 参数
	 */
	private void removeContainerCmd(Map<String, Object> parameter) {
		String containerId = (String) parameter.get("containerId");
		DockerClient dockerClient = DockerUtil.build(parameter);
		try {
			DockerClientUtil.removeContainerCmd(dockerClient, containerId);
		} finally {
			IoUtil.close(dockerClient);
		}
	}

}
