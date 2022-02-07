package io.jpom;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import com.github.dockerjava.api.DockerClient;
import io.jpom.util.LogRecorder;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * @author bwcx_jzy
 * @since 2022/2/7
 */
public class DockerClientUtil {


	public static void copyArchiveFromContainerCmd(DockerClient dockerClient, String containerId, LogRecorder logRecorder, String resultFile, String resultFileOut) {
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
