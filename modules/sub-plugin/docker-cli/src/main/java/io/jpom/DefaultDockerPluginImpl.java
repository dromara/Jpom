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
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.MD5;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.*;
import io.jpom.plugin.IDefaultPlugin;
import io.jpom.plugin.PluginConfig;
import io.jpom.util.LogRecorder;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author bwcx_jzy
 * @since 2022/1/26
 */
@PluginConfig(name = "docker-cli")
public class DefaultDockerPluginImpl implements IDefaultPlugin {

	@Override
	public Object execute(Object main, Map<String, Object> parameter) throws Exception {
		String type = main.toString();
		switch (type) {
			case "remove":
				this.removeContainerCmd(parameter);
				return null;
			case "build":
				this.build(parameter);
				return null;
			default:
				throw new IllegalArgumentException("不支持的类型");
		}
	}


	public void build(Map<String, Object> parameter) {
		DockerClient dockerClient = DockerUtil.build(parameter, 10);
		String logFile = (String) parameter.get("logFile");
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
		buildRunOn(dockerClient, runsOn, image, logRecorder);
		List<Mount> mounts = new ArrayList<>();
		mounts.addAll(cachePluginCheck(dockerClient, steps, buildId));
		mounts.addAll(javaPluginCheck(dockerClient, steps));
		mounts.addAll(mavenPluginCheck(dockerClient, steps));
		mounts.addAll(nodePluginCheck(dockerClient, steps));
		String containerId = this.buildNewContainer(dockerClient, parameter, mounts);
		try {
			String buildShell = generateBuildShell(steps, buildId);
			Path tempDirectory = Files.createTempDirectory("build");
			Path tempFile = Files.createFile(Paths.get(tempDirectory.toString() + File.separator + "build.sh"));
			Files.write(tempFile, buildShell.getBytes(), StandardOpenOption.WRITE);
			dockerClient.copyArchiveToContainerCmd(containerId)
					.withHostResource(tempFile.toAbsolutePath().toString())
					.withRemotePath("/tmp/")
					.exec();
			this.copyArchiveToContainerCmd(dockerClient, containerId, copy);
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
			this.copyArchiveFromContainerCmd(dockerClient, containerId, logRecorder, resultFile, resultFileOut);
		} catch (IOException e) {
			logRecorder.error("创建临时文件异常:", e);
		} finally {
			this.removeContainerCmd(dockerClient, containerId);
		}
	}

	private String generateBuildShell(List<Map<String, Object>> steps, String buildId) {
		StringBuilder stepsScript = new StringBuilder("#!/bin/bash\n");
		stepsScript.append("echo \"<<<<<<< Build Start >>>>>>>\"\n");
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
				}
				if ("java".equals(uses)) {
					stepsScript.append(javaScript(step));
				}
				if ("maven".equals(uses)) {
					stepsScript.append(mavenScript(step));
				}
				if ("cache".equals(uses)) {
					stepsScript.append(cacheScript(step, buildId));
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
		String path = String.format("/opt/jpom_cache_%s_%s", buildId, MD5.create().digestHex(cachePath));
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

	private String javaScript(Map<String, Object> step) {
		String version = String.valueOf(step.get("version"));
		String path = String.format("/opt/jpom_java_%s", version);
		String script = "# javaScript\n";
		script += String.format("echo \"export JAVA_HOME=%s\" >> /etc/profile\n", path);
		script += String.format("echo \"export PATH=%s/bin:$PATH\" >> /etc/profile\n", path);
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

	private void buildRunOn(DockerClient dockerClient, String runsOn, String image, LogRecorder logRecorder) {
		try {
			dockerClient.inspectImageCmd(image).exec();
		} catch (NotFoundException e) {
			HashSet<String> tags = new HashSet<>();
			tags.add(image);
			try {
				dockerClient.buildImageCmd(new File(runsOn + "/Dockerfile"))
						.withTags(tags)
						.exec(new ResultCallback.Adapter<>()).awaitCompletion();
			} catch (InterruptedException ex) {
				logRecorder.error("构建 runsOn 镜像被中断", ex);
			}
		}
	}

	private List<Mount> cachePluginCheck(DockerClient dockerClient, List<Map<String, Object>> steps, String buildId) {
		List<Mount> mounts = new ArrayList<>();
		steps.stream().filter(stringObjectMap -> stringObjectMap.containsKey("uses") && "cache".equals(String.valueOf(stringObjectMap.get("uses")))).forEach(stringObjectMap -> {
			String name = String.format("jpom_cache_%s_%s", buildId, MD5.create().digestHex(String.valueOf(stringObjectMap.get("path"))));
			mounts.add(new Mount().withType(MountType.VOLUME).withSource(name).withTarget("/opt/" + name));
			try {
				dockerClient.inspectVolumeCmd(name).exec();
			} catch (NotFoundException e) {
				dockerClient.createVolumeCmd().withName(name).exec();
			}
		});
		return mounts;
	}

	private List<Mount> javaPluginCheck(DockerClient dockerClient, List<Map<String, Object>> steps) {
		List<Mount> mounts = new ArrayList<>();
		steps.stream().filter(stringObjectMap -> stringObjectMap.containsKey("uses") && "java".equals(String.valueOf(stringObjectMap.get("uses")))).forEach(stringObjectMap -> {
			String version = String.valueOf(stringObjectMap.get("version"));
			String name = String.format("jpom_java_%s", version);
			mounts.add(new Mount().withType(MountType.VOLUME).withSource(name).withTarget("/opt/" + name));
			try {
				dockerClient.inspectVolumeCmd(name).exec();
			} catch (NotFoundException e) {
				dockerClient.createVolumeCmd().withName(name).exec();
				ArrayList<Mount> mounts2 = new ArrayList<>();
				Mount mount = new Mount().withType(MountType.VOLUME)
						.withSource(name)
						.withTarget("/opt/java");
				mounts2.add(mount);
				HostConfig hostConfig = new HostConfig()
						.withAutoRemove(true).withMounts(mounts2);
				CreateContainerResponse createContainerResponse = dockerClient.createContainerCmd("jpomdocker/runs_ubuntu-latest")
						.withHostConfig(hostConfig)
						.withEnv("JAVA_VERSION=" + version)
						.withEntrypoint("/bin/bash", "/tmp/install.sh").exec();
				dockerClient.copyArchiveToContainerCmd(createContainerResponse.getId())
						.withHostResource(new File("uses/java/install.sh").getAbsolutePath())
						.withRemotePath("/tmp/")
						.exec();
				dockerClient.startContainerCmd(createContainerResponse.getId()).exec();
			}
		});
		return mounts;
	}

	private List<Mount> mavenPluginCheck(DockerClient dockerClient, List<Map<String, Object>> steps) {
		List<Mount> mounts = new ArrayList<>();
		steps.stream().filter(stringObjectMap -> stringObjectMap.containsKey("uses") && "maven".equals(String.valueOf(stringObjectMap.get("uses")))).forEach(stringObjectMap -> {
			String version = String.valueOf(stringObjectMap.get("version"));
			String name = String.format("jpom_maven_%s", version);
			mounts.add(new Mount().withType(MountType.VOLUME).withSource(name).withTarget("/opt/" + name));
			try {
				dockerClient.inspectVolumeCmd(name).exec();
			} catch (NotFoundException e) {
				dockerClient.createVolumeCmd().withName(name).exec();
				ArrayList<Mount> mounts2 = new ArrayList<>();
				Mount mount = new Mount().withType(MountType.VOLUME)
						.withSource(name)
						.withTarget("/opt/maven");
				mounts2.add(mount);
				HostConfig hostConfig = new HostConfig()
						.withAutoRemove(true).withMounts(mounts2);
				CreateContainerResponse createContainerResponse = dockerClient.createContainerCmd("jpomdocker/runs_ubuntu-latest")
						.withHostConfig(hostConfig)
						.withEnv("MAVEN_VERSION=" + version)
						.withEntrypoint("/bin/bash", "/tmp/install.sh").exec();
				dockerClient.copyArchiveToContainerCmd(createContainerResponse.getId())
						.withHostResource(new File("uses/maven/install.sh").getAbsolutePath())
						.withRemotePath("/tmp/")
						.exec();
				dockerClient.startContainerCmd(createContainerResponse.getId()).exec();
			}
		});
		return mounts;
	}

	private List<Mount> nodePluginCheck(DockerClient dockerClient, List<Map<String, Object>> steps) {
		List<Mount> mounts = new ArrayList<>();
		steps.stream().filter(stringObjectMap -> stringObjectMap.containsKey("uses") && "node".equals(String.valueOf(stringObjectMap.get("uses")))).forEach(stringObjectMap -> {
			String version = String.valueOf(stringObjectMap.get("version"));
			String name = String.format("jpom_node_%s", version);
			mounts.add(new Mount().withType(MountType.VOLUME).withSource(name).withTarget("/opt/" + name));
			try {
				dockerClient.inspectVolumeCmd(name).exec();
			} catch (NotFoundException e) {
				dockerClient.createVolumeCmd().withName(name).exec();
				ArrayList<Mount> mounts2 = new ArrayList<>();
				Mount mount = new Mount().withType(MountType.VOLUME)
						.withSource(name)
						.withTarget("/opt/node");
				mounts2.add(mount);
				HostConfig hostConfig = new HostConfig()
						.withAutoRemove(true).withMounts(mounts2);
				CreateContainerResponse createContainerResponse = dockerClient.createContainerCmd("jpomdocker/runs_ubuntu-latest")
						.withHostConfig(hostConfig)
						.withEnv("NODE_VERSION=" + version)
						.withEntrypoint("/bin/bash", "/tmp/install.sh").exec();
				dockerClient.copyArchiveToContainerCmd(createContainerResponse.getId())
						.withHostResource(new File("uses/node/install.sh").getAbsolutePath())
						.withRemotePath("/tmp/")
						.exec();
				dockerClient.startContainerCmd(createContainerResponse.getId()).exec();
			}
		});
		return mounts;
	}

	private void pullLog(DockerClient dockerClient, String containerId, LogRecorder logRecorder) {
		// 获取日志
		try {
			dockerClient.logContainerCmd(containerId)
					.withStdOut(true)
					.withStdErr(true)
					.withTailAll()
					.withFollowStream(true)
					.exec(new ResultCallback.Adapter<Frame>() {
						@Override
						public void onNext(Frame object) {
							String s = new String(object.getPayload(), StandardCharsets.UTF_8);
							logRecorder.append(s);
						}
					}).awaitCompletion();
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


	private void copyArchiveFromContainerCmd(DockerClient dockerClient, String containerId, LogRecorder logRecorder, String resultFile, String resultFileOut) {
		try (InputStream stream = dockerClient.copyArchiveFromContainerCmd(containerId, resultFile).exec();
			 TarArchiveInputStream tarStream = new TarArchiveInputStream(stream)) {
			TarArchiveEntry tarArchiveEntry;
			while ((tarArchiveEntry = tarStream.getNextTarEntry()) != null) {
				if (!tarStream.canReadEntryData(tarArchiveEntry)) {
					logRecorder.info("不能读取tarArchiveEntry");
				}
				if (tarArchiveEntry.isDirectory()) {
					continue;
				}
				String archiveEntryName = tarArchiveEntry.getName();
				// 截取第一级目录
				archiveEntryName = StrUtil.subAfter(archiveEntryName, StrUtil.SLASH, false);
				// 可能中包含文件 使用原名称
				archiveEntryName = StrUtil.emptyToDefault(archiveEntryName, tarArchiveEntry.getName());
				//logRecorder.info("tarArchiveEntry's name: {}", archiveEntryName);
				File currentFile = FileUtil.file(resultFileOut, archiveEntryName);
				FileUtil.mkParentDirs(currentFile);
				IoUtil.copy(tarStream, new FileOutputStream(currentFile));
			}
		} catch (Exception e) {
			logRecorder.error("无法获取容器执行结果文件: {}", e);
		}
	}


	private String buildNewContainer(DockerClient dockerClient, Map<String, Object> parameter, List<Mount> mounts) {
		String image = (String) parameter.get("image");
		String workingDir = (String) parameter.get("workingDir");
		String dockerName = (String) parameter.get("dockerName");

		Map<String, String> env = (Map<String, String>) parameter.get("env");
		CreateContainerCmd containerCmd = dockerClient.createContainerCmd(image);
		containerCmd.withName(dockerName).withWorkingDir(workingDir);
		HostConfig hostConfig = HostConfig.newHostConfig()
				.withMounts(mounts);
		containerCmd.withHostConfig(hostConfig);


		if (env != null) {
			List<String> envList = env.entrySet().stream().map(stringStringEntry -> StrUtil.format("{}={}", stringStringEntry.getKey(), stringStringEntry.getValue())).collect(Collectors.toList());
			containerCmd.withEnv(envList);
		}
		try {
			InspectContainerResponse exec = dockerClient.inspectContainerCmd(dockerName).exec();
			this.removeContainerCmd(dockerClient, exec.getId());
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
	private void copyArchiveToContainerCmd(DockerClient dockerClient, String containerId, List<String> copy) {
		if (copy == null || dockerClient == null) {
			return;
		}
		for (String s : copy) {
			List<String> split = StrUtil.split(s, StrUtil.COLON);
			dockerClient.copyArchiveToContainerCmd(containerId)
					.withHostResource(split.get(0))
					.withRemotePath(split.get(1))
					.withDirChildrenOnly(Convert.toBool(CollUtil.get(split, 2), true))
					.exec();
		}
	}

	/**
	 * 删除容器
	 *
	 * @param parameter 参数
	 */
	private void removeContainerCmd(Map<String, Object> parameter) {
		String containerId = (String) parameter.get("containerId");
		DockerClient dockerClient = DockerUtil.build(parameter, 10);
		removeContainerCmd(dockerClient, containerId);
	}

	/**
	 * 删除容器
	 *
	 * @param dockerClient docker 连接
	 * @param containerId  容器ID
	 */
	private void removeContainerCmd(DockerClient dockerClient, String containerId) {
		if (containerId == null) {
			return;
		}
		// 清除容器
		dockerClient.removeContainerCmd(containerId)
				.withRemoveVolumes(true)
				.withForce(true)
				.exec();
	}
}
