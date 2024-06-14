/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Frame;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.util.LogRecorder;
import org.springframework.util.Assert;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Optional;
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
    public static void pullLog(DockerClient dockerClient,
                               String containerId,
                               Boolean timestamps,
                               Integer tail,
                               Charset charset,
                               Consumer<String> consumer,
                               Consumer<AutoCloseable> consumerAdapter) throws InterruptedException {
        Assert.state(tail == null || tail > 0, "tail > 0");
        // 获取日志
        LogContainerCmd logContainerCmd = dockerClient.logContainerCmd(containerId);
        if (tail == null) {
            logContainerCmd.withTailAll();
        } else {
            logContainerCmd.withTail(tail);
        }
        ResultCallback.Adapter<Frame> exec = logContainerCmd
            .withTimestamps(timestamps)
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
            });
        Optional.ofNullable(consumerAdapter).ifPresent(consumer1 -> consumer1.accept(exec));
        //
        exec.awaitCompletion();
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
        logRecorder.system("download file from : {}", resultFile);
        File tmpDir = FileUtil.getTmpDir();
        File fileArchive = FileUtil.file(tmpDir, "jpom", "docker-temp-archive", containerId);
        try {
            try (InputStream stream = dockerClient.copyArchiveFromContainerCmd(containerId, resultFile).exec();
                 TarArchiveInputStream tarStream = new TarArchiveInputStream(stream)) {
                TarArchiveEntry tarArchiveEntry;
                while ((tarArchiveEntry = tarStream.getNextEntry()) != null) {
                    if (!tarStream.canReadEntryData(tarArchiveEntry)) {
                        logRecorder.systemWarning(I18nMessageUtil.get("i18n.cannot_read_tar_archive_entry.85d7"), tarArchiveEntry.getName());
                    }
                    if (tarArchiveEntry.isDirectory()) {
                        continue;
                    }
                    String archiveEntryName = tarArchiveEntry.getName();
                    // 截取第一级目录
                    archiveEntryName = StrUtil.subAfter(archiveEntryName, StrUtil.SLASH, false);
                    // 可能中包含文件 使用原名称
                    archiveEntryName = StrUtil.emptyToDefault(archiveEntryName, tarArchiveEntry.getName());
                    File currentFile = FileUtil.file(fileArchive, archiveEntryName);
                    FileUtil.mkParentDirs(currentFile);
                    FileUtil.writeFromStream(tarStream, currentFile, false);
                }
            } catch (NotFoundException notFoundException) {
                logRecorder.systemWarning(I18nMessageUtil.get("i18n.execution_result_file_not_found_in_container.cf18"), notFoundException.getMessage());
            } catch (Exception e) {
                logRecorder.error(I18nMessageUtil.get("i18n.unable_to_get_container_execution_result_file.7b2c"), e);
            }
            // github pr 71
            // https://github.com/dromara/Jpom/pull/71
            File[] files = fileArchive.listFiles();
            if (files == null) {
                logRecorder.systemWarning(I18nMessageUtil.get("i18n.temporary_result_file_does_not_exist.1c7e"), fileArchive.getAbsolutePath());
                return;
            }
            File resultFileOutFile = FileUtil.file(resultFileOut);
            if (ArrayUtil.length(files) == 1) {
                FileUtil.mkParentDirs(resultFileOutFile);
                FileUtil.move(files[0], resultFileOutFile, true);
            } else {
                FileUtil.mkdir(resultFileOutFile);
                FileUtil.moveContent(fileArchive, resultFileOutFile, true);
            }
        } finally {
            FileUtil.del(fileArchive);
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
