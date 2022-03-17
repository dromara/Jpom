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
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.*;
import io.jpom.util.LogRecorder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 容器构建
 *
 * @author bwcx_jzy
 * @since 2022/2/7
 */
public class DockerBuild implements AutoCloseable {

	private static final String[] DEPEND_PLUGIN = new String[]{"java", "maven", "node", "go", "python3"};

	private final Map<String, Object> parameter;
	private final DockerClient dockerClient;

	public DockerBuild(Map<String, Object> parameter) {
		this.parameter = parameter;
		this.dockerClient = DockerUtil.build(parameter, 10);
	}

	public void build() {

		String logFile = (String) parameter.get("logFile");
		File tempDir = (File) parameter.get("tempDir");
		// 生成临时目录
		tempDir = FileUtil.file(tempDir, "docker-temp", IdUtil.fastSimpleUUID());
		LogRecorder logRecorder = LogRecorder.builder().filePath(logFile).build();
		List<String> copy = (List<String>) parameter.get("copy");
		String resultFile = (String) parameter.get("resultFile");
		String resultFileOut = (String) parameter.get("resultFileOut");
		List<Map<String, Object>> steps = (List<Map<String, Object>>) parameter.get("steps");
		Map<String, String> env = (Map<String, String>) parameter.get("env");
		String buildId = env.get("JPOM_BUILD_ID");
		String runsOn = (String) parameter.get("runsOn");

		String image = String.format("jpomdocker/runs_%s", runsOn);
		parameter.put("image", image);
		String containerId = null;
		try {
			this.buildRunOn(dockerClient, runsOn, image, tempDir, logRecorder);
			List<Mount> mounts = new ArrayList<>();
			// 缓存目录
			List<Mount> cacheMount = this.cachePluginCheck(dockerClient, steps, buildId);
			mounts.addAll(cacheMount);
			// 添加插件到 mount
			mounts.addAll(this.checkDependPlugin(dockerClient, image, steps, buildId, tempDir, logRecorder));
			containerId = this.buildNewContainer(dockerClient, parameter, mounts);

			String buildShell = this.generateBuildShell(steps, buildId);
			File tempFile = DockerUtil.createTemp("build.sh", tempDir);
			FileUtil.writeUtf8String(buildShell, tempFile);
			dockerClient.copyArchiveToContainerCmd(containerId)
					.withHostResource(tempFile.getAbsolutePath())
					.withRemotePath("/tmp/")
					.exec();
			//
			copy = this.replaceEnv(copy, env);
			this.copyArchiveToContainerCmd(dockerClient, containerId, copy, logRecorder);
			// 启动容器
			try {
				dockerClient.startContainerCmd(containerId).exec();
			} catch (RuntimeException e) {
				logRecorder.error("容器启动失败:", e);
				return;
			}
			// 获取日志
			this.pullLog(dockerClient, containerId, logRecorder);
			// 等待容器执行结果
			this.waitContainerCmd(dockerClient, containerId, logRecorder);
			// 获取容器执行结果文件
			DockerClientUtil.copyArchiveFromContainerCmd(dockerClient, containerId, logRecorder, resultFile, resultFileOut);
		} finally {
			DockerClientUtil.removeContainerCmd(dockerClient, containerId);
			// 删除临时目录
			FileUtil.del(tempDir);
		}
	}

	private List<String> replaceEnv(List<String> list, Map<String, String> env) {
		return list.stream().map(s -> {
			// 处理变量
			for (Map.Entry<?, ?> envEntry : env.entrySet()) {
				String envValue = StrUtil.utf8Str(envEntry.getValue());
				if (null == envValue) {
					continue;
				}
				s = StrUtil.replace(s, "${" + envEntry.getKey() + "}", envValue);
			}
			return s;
		}).collect(Collectors.toList());
	}


	/**
	 * 构建一个执行 镜像
	 *
	 * @param dockerClient docker 客户端连接
	 * @param parameter    参数
	 * @param mounts       挂载目录
	 * @return 容器ID
	 */
	private String buildNewContainer(DockerClient dockerClient, Map<String, Object> parameter, List<Mount> mounts) {
		String image = (String) parameter.get("image");
		String workingDir = (String) parameter.get("workingDir");
		String dockerName = (String) parameter.get("dockerName");
		List<String> binds = (List<String>) parameter.get("binds");

		Map<String, String> env = (Map<String, String>) parameter.get("env");
		CreateContainerCmd containerCmd = dockerClient.createContainerCmd(image);
		containerCmd.withName(dockerName).withWorkingDir(workingDir);
		//
		List<Bind> bindList = new ArrayList<>();
		if (CollUtil.isNotEmpty(binds)) {
			bindList = this.replaceEnv(binds, env).stream()
					.map(Bind::parse)
					.collect(Collectors.toList());
		}

		HostConfig hostConfig = HostConfig.newHostConfig()
				.withMounts(mounts).withBinds(bindList);
		containerCmd.withHostConfig(hostConfig);
		// 环境变量
		if (env != null) {
			List<String> envList = env.entrySet()
					.stream()
					.map(entry -> StrUtil.format("{}={}", entry.getKey(), entry.getValue()))
					.collect(Collectors.toList());
			containerCmd.withEnv(envList);
		}
		// 如果容器已经存在则先删除
		try {
			InspectContainerResponse exec = dockerClient.inspectContainerCmd(dockerName).exec();
			DockerClientUtil.removeContainerCmd(dockerClient, exec.getId());
		} catch (NotFoundException ignored) {
		}
		// 创建容器
		CreateContainerResponse containerResponse;
		containerResponse = containerCmd.exec();
		return containerResponse.getId();
	}

	/**
	 * 将本地 文件 上传到 容器
	 *
	 * @param dockerClient docker 连接
	 * @param containerId  容器ID
	 * @param copy         需要 上传到文件信息
	 */
	private void copyArchiveToContainerCmd(DockerClient dockerClient, String containerId, List<String> copy, LogRecorder logRecorder) {
		if (copy == null || dockerClient == null) {
			return;
		}
		for (String s : copy) {
			List<String> split = StrUtil.split(s, StrUtil.COLON);
			logRecorder.info("send file to : {}\n", split.get(1));
			dockerClient.copyArchiveToContainerCmd(containerId)
					.withHostResource(split.get(0))
					.withRemotePath(split.get(1))
					.withDirChildrenOnly(Convert.toBool(CollUtil.get(split, 2), true))
					.exec();
		}
	}

	/**
	 * 生成执行构建的命令
	 *
	 * @param steps   执行步骤
	 * @param buildId 构建ID
	 * @return sh
	 */
	private String generateBuildShell(List<Map<String, Object>> steps, String buildId) {
		StringBuilder stepsScript = new StringBuilder("#!/bin/bash\n");
		stepsScript.append("echo \"<<<<<<< Build Start >>>>>>>\"\n");
		//
		for (Map<String, Object> step : steps) {
			if (step.containsKey("env")) {
				stepsScript.append("# env\n");
				Map<String, String> env = (Map<String, String>) step.get("env");
				for (String key : env.keySet()) {
					stepsScript.append(String.format("echo \"export %s=%s\" >> /etc/profile\n", key, env.get(key)));
				}
			}
			stepsScript.append("source /etc/profile \n");
			if (step.containsKey("uses")) {
				String uses = (String) step.get("uses");
				if ("node".equals(uses)) {
					stepsScript.append(nodeScript(step));
				} else if ("java".equals(uses)) {
					stepsScript.append(javaScript(step));
				} else if ("maven".equals(uses)) {
					stepsScript.append(mavenScript(step));
				} else if ("cache".equals(uses)) {
					stepsScript.append(cacheScript(step, buildId));
				} else if ("go".equals(uses)) {
					stepsScript.append(goScript(step));
				} else if ("python3".equals(uses)) {
					stepsScript.append(python3Script(step));
				}
			}
			if (step.containsKey("run")) {
				stepsScript.append("# run\n");
				String run = (String) step.get("run");
				stepsScript.append(run).append(" \n");
			}
		}
		stepsScript.append("echo \"<<<<<<< Build End >>>>>>>\"\n");
		return stepsScript.toString();
	}

	private String cacheScript(Map<String, Object> step, String buildId) {
		String cachePath = String.valueOf(step.get("path"));
		String path = String.format("/opt/jpom_cache_%s_%s", buildId, SecureUtil.md5(cachePath));
		String script = "# cacheScript\n";
		script += String.format("ln -s %s %s \n", path, cachePath);
		return script;
	}

	private String mavenScript(Map<String, Object> step) {
		String version = String.valueOf(step.get("version"));
		String path = String.format("/opt/jpom_maven_%s", version);
		String script = "# mavenScript\n";
		script += String.format("echo \"export MAVEN_HOME=%s\" >> /etc/profile\n", path);
		script += String.format("echo \"export PATH=%s/bin:$PATH\" >> /etc/profile\n", path);
		script += "source /etc/profile \n";
		return script;
	}

	private String goScript(Map<String, Object> step) {
		String version = String.valueOf(step.get("version"));
		String path = String.format("/opt/jpom_go_%s", version);
		String script = "# goScript\n";
		script += String.format("echo \"export GOROOT=%s\" >> /etc/profile\n", path);
		script += String.format("echo \"export PATH=$PATH:%s/bin\" >> /etc/profile\n", path);
		script += "source /etc/profile \n";
		return script;
	}

	private String javaScript(Map<String, Object> step) {
		String version = String.valueOf(step.get("version"));
		String path = String.format("/opt/jpom_java_%s", version);
		String script = "# javaScript\n";
		script += String.format("echo \"export JAVA_HOME=%s\" >> /etc/profile\n", path);
		script += String.format("echo \"export PATH=%s/bin:$PATH\" >> /etc/profile\n", path);
		script += "source /etc/profile \n";
		return script;
	}

	private String python3Script(Map<String, Object> step) {
		String version = String.valueOf(step.get("version"));
		String path = String.format("/opt/jpom_python3_%s", version);
		String script = "# python3Script\n";
		script += String.format("echo \"export PYTHONPATH=%s\" >> /etc/profile\n", path);
		script += String.format("echo \"export PATH=%s/bin:$PATH\" >> /etc/profile\n", path);
		// pip3 `bad interpreter: no such file or directory:`
		script += String.format("sed -i \"1c#!%s/bin/python3\" %s/bin/pip3\n", path, path);
		script += "source /etc/profile \n";
		return script;
	}

	private String nodeScript(Map<String, Object> step) {
		String version = String.valueOf(step.get("version"));
		String path = String.format("/opt/jpom_node_%s", version);
		String script = "# nodeScript\n";
		script += String.format("echo \"export NODE_HOME=%s\" >> /etc/profile\n", path);
		script += String.format("echo \"export PATH=%s/bin:$PATH\" >> /etc/profile\n", path);
		script += "source /etc/profile \n";
		return script;
	}

	/**
	 * 插件 runsOn 镜像
	 *
	 * @param dockerClient docker 连接
	 * @param runsOn       基于容器镜像
	 * @param image        镜像
	 * @param tempDir      临时路径
	 * @param logRecorder  日志记录
	 */
	private void buildRunOn(DockerClient dockerClient, String runsOn, String image, File tempDir, LogRecorder logRecorder) {
		try {
			dockerClient.inspectImageCmd(image).exec();
		} catch (NotFoundException e) {
			HashSet<String> tags = new HashSet<>();
			tags.add(image);
			try {
				File file = DockerUtil.getResourceToFile(runsOn + "/Dockerfile", tempDir);
				if (file == null) {
					throw new IllegalArgumentException("当前还不支持：" + runsOn);
				}
				dockerClient.buildImageCmd(file)
						.withTags(tags)
						.exec(new ResultCallback.Adapter<BuildResponseItem>() {
							@Override
							public void onNext(BuildResponseItem object) {
								String responseItem = DockerUtil.parseResponseItem(object);
								logRecorder.append(responseItem);
							}
						}).awaitCompletion();
			} catch (InterruptedException ex) {
				logRecorder.error("构建 runsOn 镜像被中断", ex);
			}
		}
	}

	private List<Mount> cachePluginCheck(DockerClient dockerClient, List<Map<String, Object>> steps, String buildId) {
		return steps.stream()
				.filter(map -> {
					String uses = (String) map.get("uses");
					return StrUtil.equals("cache", uses);
				})
				.map(stringObjectMap -> {
					String path = (String) stringObjectMap.get("path");
					String name = String.format("jpom_cache_%s_%s", buildId, SecureUtil.md5(path));
					try {
						dockerClient.inspectVolumeCmd(name).exec();
					} catch (NotFoundException e) {
						HashMap<String, String> labels = MapUtil.of("jpom_build_" + buildId, buildId);
						labels.put("jpom_build_path", path);
						dockerClient.createVolumeCmd()
								.withName(name)
								.withLabels(labels)
								.exec();
					}
					Mount mount = new Mount();
					mount.withType(MountType.VOLUME).withSource(name).withTarget("/opt/" + name);
					return mount;
				}).collect(Collectors.toList());
	}

	/**
	 * 依赖插件检查
	 *
	 * @param dockerClient docker 客户端连接
	 * @param image        执行镜像名称
	 * @param steps        相关参数
	 * @param tempDir      临时文件目录
	 * @param logRecorder  日志记录器
	 * @param buildId      构建ID
	 * @return list mount
	 */
	private List<Mount> checkDependPlugin(DockerClient dockerClient, String image, List<Map<String, Object>> steps, String buildId, File tempDir, LogRecorder logRecorder) {
		return steps.stream()
				.filter(map -> {
					String uses = (String) map.get("uses");
					return ArrayUtil.contains(DEPEND_PLUGIN, uses);
				})
				.map(map -> this.dependPluginCheck(dockerClient, image, map, buildId, tempDir, logRecorder))
				.collect(Collectors.toList());
	}

	/**
	 * 依赖插件检查
	 *
	 * @param dockerClient docker 客户端连接
	 * @param image        执行镜像名称
	 * @param usesMap      插件相关参数
	 * @param tempDir      临时文件目录
	 * @param logRecorder  日志记录器
	 * @param buildId      构建ID
	 * @return mount
	 */
	private Mount dependPluginCheck(DockerClient dockerClient, String image, Map<String, Object> usesMap, String buildId, File tempDir, LogRecorder logRecorder) {
		String pluginName = (String) usesMap.get("uses");
		String version = String.valueOf(usesMap.get("version"));
		String name = String.format("jpom_%s_%s", pluginName, version);

		try {
			dockerClient.inspectVolumeCmd(name).exec();
		} catch (NotFoundException e) {
			dockerClient.createVolumeCmd()
					.withName(name)
					.withLabels(MapUtil.of("jpom_build_" + buildId, buildId))
					.exec();

			Mount mount = new Mount().withType(MountType.VOLUME)
					.withSource(name)
					.withTarget("/opt/" + pluginName);

			HostConfig hostConfig = new HostConfig()
					.withAutoRemove(true).withMounts(CollUtil.newArrayList(mount));
			CreateContainerResponse createContainerResponse = dockerClient.createContainerCmd(image)
					.withHostConfig(hostConfig)
					.withName(name)
					.withEnv(pluginName.toUpperCase() + "_VERSION=" + version)
					.withLabels(MapUtil.of("jpom_build_" + buildId, buildId))
					.withEntrypoint("/bin/bash", "/tmp/install.sh").exec();
			String containerId = createContainerResponse.getId();
			// 将脚本 复制到容器
			String pluginInstallResource = DockerUtil.getResourceToFilePath("uses/" + pluginName + "/install.sh", tempDir);
			if (pluginInstallResource == null) {
				throw new IllegalArgumentException("当前还不支持：" + pluginName);
			}
			dockerClient.copyArchiveToContainerCmd(containerId)
					.withHostResource(pluginInstallResource)
					.withRemotePath("/tmp/")
					.exec();
			//
			dockerClient.startContainerCmd(containerId).exec();
			// 获取日志
			this.pullLog(dockerClient, containerId, logRecorder);
		}
		return new Mount().withType(MountType.VOLUME).withSource(name).withTarget("/opt/" + name);
	}


	private void pullLog(DockerClient dockerClient, String containerId, LogRecorder logRecorder) {
		// 获取日志
		try {
			DockerClientUtil.pullLog(dockerClient, containerId, null, StandardCharsets.UTF_8, logRecorder::append);
		} catch (InterruptedException e) {
			logRecorder.error("获取容器日志操作被中断:", e);
		} catch (RuntimeException e) {
			logRecorder.error("获取容器日志失败", e);
		}
	}


	private void waitContainerCmd(DockerClient dockerClient, String containerId, LogRecorder logRecorder) {
		try {
			dockerClient.waitContainerCmd(containerId).exec(new ResultCallback.Adapter<WaitResponse>() {
				@Override
				public void onNext(WaitResponse object) {
					logRecorder.info("dockerTask status code is: {}", object.getStatusCode());
				}
			}).awaitCompletion();
		} catch (InterruptedException e) {
			logRecorder.error("获取容器执行结果操作被中断:", e);
		} catch (RuntimeException e) {
			logRecorder.error("获取容器执行结果失败", e);
		}
	}

	@Override
	public void close() throws Exception {
		IoUtil.close(dockerClient);
	}
}
