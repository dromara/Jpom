package io.jpom;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.*;
import io.jpom.plugin.IDefaultPlugin;
import io.jpom.plugin.PluginConfig;
import io.jpom.util.LogRecorder;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
		if (StrUtil.equals(type, "build")) {
			this.build(parameter);
			return null;
		}
		throw new IllegalArgumentException("不支持的类型");
	}


	public void build(Map<String, Object> parameter) {
		DockerClient dockerClient = DockerUtil.build(parameter, 10);

		String image = (String) parameter.get("image");
		String workingDir = (String) parameter.get("workingDir");
		String dockerName = (String) parameter.get("dockerName");
		String logFile = (String) parameter.get("logFile");
		LogRecorder logRecorder = LogRecorder.builder().filePath(logFile).build();
		List<String> binds = (List<String>) parameter.get("binds");
		List<String> entrypoint = (List<String>) parameter.get("entrypoints");
		List<String> cmds = (List<String>) parameter.get("cmds");
		Map<String, String> env = (Map<String, String>) parameter.get("env");
		//String resultFile = (String) parameter.get("resultFile");
		//String resultFileOut = (String) parameter.get("resultFileOut");

		CreateContainerCmd containerCmd = dockerClient.createContainerCmd(image);
		containerCmd.withName(dockerName).withWorkingDir(workingDir);
		//
		List<Bind> bindList = new ArrayList<>();
		if (CollUtil.isNotEmpty(binds)) {
			bindList = binds.stream().map(Bind::parse).collect(Collectors.toList());
		}
		//
		//
		HostConfig hostConfig = HostConfig.newHostConfig().withBinds(bindList);
		containerCmd.withHostConfig(hostConfig);

		if (entrypoint != null) {
			containerCmd.withEntrypoint(entrypoint);
		}
		if (cmds != null) {
			containerCmd.withCmd(cmds);
		}
		if (env != null) {
			List<String> envList = env.entrySet().stream().map(stringStringEntry -> StrUtil.format("{}={}", stringStringEntry.getKey(), stringStringEntry.getValue())).collect(Collectors.toList());
			containerCmd.withEnv(envList);
		}
		//
		// 检查镜像是否存在本地
		boolean imagePull = false;
		try {
			dockerClient.inspectImageCmd(image).exec();
		} catch (NotFoundException e) {
			logRecorder.info("镜像不存在，需要下载");
			imagePull = true;
		}
		// 拉取镜像
		if (imagePull) {
			try {
				dockerClient.pullImageCmd(image).exec(new ResultCallback.Adapter<PullResponseItem>() {
					@Override
					public void onNext(PullResponseItem object) {
						logRecorder.info("镜像下载成功: {} status: {}", object.getId(), object.getStatus());
					}
				}).awaitCompletion();
			} catch (InterruptedException | RuntimeException e) {
				logRecorder.error("镜像下载失败:", e);
				return;
			}
		}
		//
		// 创建容器
		CreateContainerResponse containerResponse;
		try {
			containerResponse = containerCmd.exec();
		} catch (RuntimeException e) {
			logRecorder.error("无法创建容器", e);
			return;
		}
		String containerId = containerResponse.getId();
		try {
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
			//this.copyFile(dockerClient, containerId, logRecorder, resultFile, resultFileOut);
		} finally {
			this.removeContainerCmd(dockerClient, containerId);
		}
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


	private void copyFile(DockerClient dockerClient, String containerId, LogRecorder logRecorder, String resultFile, String resultFileOut) {
		logRecorder.info("pre mv {} {}", resultFile, resultFileOut);
		try (InputStream stream = dockerClient.copyArchiveFromContainerCmd(containerId, resultFile).exec();
			 TarArchiveInputStream tarStream = new TarArchiveInputStream(stream);) {
			TarArchiveEntry tarArchiveEntry;
			File file1 = FileUtil.file(resultFileOut, resultFile);
			FileUtil.del(file1);

			while ((tarArchiveEntry = tarStream.getNextTarEntry()) != null) {
				if (!tarStream.canReadEntryData(tarArchiveEntry)) {
					logRecorder.info("不能读取tarArchiveEntry");
				}
				if (tarArchiveEntry.isDirectory()) {
					continue;
				}
				logRecorder.info("tarArchiveEntry's name: {}", tarArchiveEntry.getName());
				File currentFile = FileUtil.file(resultFileOut, tarArchiveEntry.getName());
				FileUtil.mkParentDirs(currentFile);
				IoUtil.copy(tarStream, new FileOutputStream(currentFile));
			}
		} catch (Exception e) {
			logRecorder.error("无法获取容器执行结果文件: {}", e);
		}
	}


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
