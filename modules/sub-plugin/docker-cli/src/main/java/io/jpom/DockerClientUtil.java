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

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Frame;
import io.jpom.util.LogRecorder;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.springframework.util.Assert;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.function.Consumer;

/**
 * @author bwcx_jzy
 * @since 2022/2/7
 */
public class DockerClientUtil {

	/**
	 * 拉取容器日志
	 *
	 * @param dockerClient 连接
	 * @param containerId  容器ID
	 * @param charset      字符编码
	 * @param consumer     回调
	 * @throws InterruptedException 打断异常
	 */
	public static void pullLog(DockerClient dockerClient, String containerId, Integer tail, Charset charset, Consumer<String> consumer) throws InterruptedException {
		Assert.state(tail == null || tail > 0, "tail > 0");
		// 获取日志
		LogContainerCmd logContainerCmd = dockerClient.logContainerCmd(containerId);
		if (tail == null) {
			logContainerCmd.withTailAll();
		} else {
			logContainerCmd.withTail(tail);
		}
		logContainerCmd
				.withStdOut(true)
				.withStdErr(true)
				.withFollowStream(true)
				.exec(new ResultCallback.Adapter<Frame>() {
					@Override
					public void onNext(Frame object) {
						byte[] payload = object.getPayload();
						if (payload == null) {
							return;
						}
						String s = new String(payload, charset);
						consumer.accept(s);
					}
				}).awaitCompletion();
	}

	/**
	 * 将容器文件下载到本地
	 *
	 * @param dockerClient  容器连接
	 * @param containerId   容器ID
	 * @param logRecorder   日志记录
	 * @param resultFile    结果文件
	 * @param resultFileOut 保存目录
	 */
	public static void copyArchiveFromContainerCmd(DockerClient dockerClient, String containerId, LogRecorder logRecorder, String resultFile, String resultFileOut) {
		logRecorder.info("download file from : {}", resultFile);
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
		} catch (NotFoundException notFoundException) {
			logRecorder.info("容器中没有找到执行结果文件: {}", notFoundException.getMessage());
		} catch (Exception e) {
			logRecorder.error("无法获取容器执行结果文件", e);
		}
	}

	/**
	 * 删除容器
	 *
	 * @param dockerClient docker 连接
	 * @param containerId  容器ID
	 */
	public static void removeContainerCmd(DockerClient dockerClient, String containerId) {
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
