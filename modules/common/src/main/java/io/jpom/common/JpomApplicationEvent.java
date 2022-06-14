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
package io.jpom.common;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.jiangzeyin.common.spring.event.ApplicationEventClient;
import cn.jiangzeyin.common.spring.event.ApplicationEventLoad;
import com.alibaba.fastjson.JSONObject;
import io.jpom.JpomApplication;
import io.jpom.system.ConfigBean;
import io.jpom.system.ExtConfigBean;
import io.jpom.util.JsonFileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ContextClosedEvent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.List;

/**
 * 启动 、关闭监听
 *
 * @author jiangzeyin
 * @since 2019/4/7
 */
@Slf4j
public class JpomApplicationEvent implements ApplicationEventClient {
	private FileLock lock;
	private FileOutputStream fileOutputStream;
	private FileChannel fileChannel;


	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		// 启动最后的预加载
		if (event instanceof ApplicationReadyEvent) {
			//
			checkPath();
			JpomManifest jpomManifest = JpomManifest.getInstance();
			ConfigBean instance = ConfigBean.getInstance();
			// 清理旧进程新文件
			File dataDir = FileUtil.file(instance.getDataPath());
			List<File> files = FileUtil.loopFiles(dataDir, 1, pathname -> pathname.getName().startsWith("pid."));
			files.forEach(FileUtil::del);
			try {
				this.lockFile(jpomManifest.getPid());
			} catch (IOException e) {
				log.error("lockFile", e);
			}
			// 写入Jpom 信息 、 写入全局信息
			File appJpomFile = instance.getApplicationJpomInfo(JpomApplication.getAppType());
			FileUtil.writeString(jpomManifest.toString(), appJpomFile, CharsetUtil.CHARSET_UTF_8);
			// 检查更新文件
			checkUpdate();
			//
			if (ApplicationEventLoad.class.isAssignableFrom(JpomApplication.getAppClass())) {
				ApplicationEventLoad eventLoad = (ApplicationEventLoad) SpringUtil.getBean(JpomApplication.getAppClass());
				eventLoad.applicationLoad();
			}
			Console.log("Jpom Successful start preparation. start loading module");
		} else if (event instanceof ContextClosedEvent) {
			// 应用关闭
			this.unLockFile();
			//
			ConfigBean instance = ConfigBean.getInstance();
			FileUtil.del(instance.getPidFile());
			//
			File appJpomFile = instance.getApplicationJpomInfo(JpomApplication.getAppType());
			FileUtil.del(appJpomFile);
		}
	}

	/**
	 * 解锁进程文件
	 */
	private void unLockFile() {
		if (lock != null) {
			try {
				lock.release();
			} catch (IOException ignored) {
			}
		}
		IoUtil.close(lock);
		IoUtil.close(fileChannel);
		IoUtil.close(fileOutputStream);
	}

	/**
	 * 锁住进程文件
	 *
	 * @throws IOException IO
	 */
	private void lockFile(long pid) throws IOException {
		this.fileOutputStream = new FileOutputStream(ConfigBean.getInstance().getPidFile(), true);
		this.fileOutputStream.write(StrUtil.bytes("Jpom pid:" + pid, CharsetUtil.CHARSET_UTF_8));
		this.fileOutputStream.flush();
		this.fileChannel = fileOutputStream.getChannel();
		while (true) {
			try {
				lock = fileChannel.lock();
				break;
			} catch (OverlappingFileLockException | IOException e) {
				log.warn("获取进程文件锁失败：" + e.getMessage());
			}
			ThreadUtil.sleep(100);
		}
	}

	private static void checkPath() {
		String path = ExtConfigBean.getInstance().getPath();
		String extConfigPath = null;
		try {
			extConfigPath = ExtConfigBean.getResource().getURL().toString();
		} catch (IOException ignored) {
		}
		File file = FileUtil.file(path);
		try {
			FileUtil.mkdir(file);
			file = FileUtil.createTempFile("jpom", ".temp", file, true);
		} catch (Exception e) {
			log.error(StrUtil.format("Jpom Failed to create data directory, directory location：{}," +
					"Please check whether the current user has permission to this directory or modify the configuration file：{} jpom.path in is the path where the directory can be created", path, extConfigPath), e);
			System.exit(-1);
		}
		FileUtil.del(file);
		Console.log("Jpom[{}] Current data path：{} External configuration file path：{}", JpomManifest.getInstance().getVersion(), path, extConfigPath);
	}

	/**
	 * 检查更新包文件状态
	 */
	private static void checkUpdate() {
		File runFile = JpomManifest.getRunPath().getParentFile();
		String upgrade = FileUtil.file(runFile, ConfigBean.UPGRADE).getAbsolutePath();
		JSONObject jsonObject = null;
		try {
			jsonObject = (JSONObject) JsonFileUtil.readJson(upgrade);
		} catch (FileNotFoundException ignored) {
		}
		if (jsonObject != null) {
			String beforeJar = jsonObject.getString("beforeJar");
			if (StrUtil.isNotEmpty(beforeJar)) {
				File beforeJarFile = FileUtil.file(runFile, beforeJar);
				if (beforeJarFile.exists()) {
					File oldJars = JpomManifest.getOldJarsPath();
					FileUtil.mkdir(oldJars);
					FileUtil.move(beforeJarFile, oldJars, true);
					log.info("备份旧程序包：" + beforeJar);
				}
			}
		}
		clearOldJar();
		// windows 备份日志
		//        if (SystemUtil.getOsInfo().isWindows()) {
		//            boolean logBack = jsonObject.getBooleanValue("logBack");
		//            String oldLogName = jsonObject.getString("oldLogName");
		//            if (logBack && StrUtil.isNotEmpty(oldLogName)) {
		//                File scriptFile = JpomManifest.getScriptFile();
		//                File oldLog = FileUtil.file(scriptFile.getParentFile(), oldLogName);
		//                if (oldLog.exists()) {
		//                    File logBackDir = FileUtil.file(scriptFile.getParentFile(), "log");
		//                    FileUtil.move(oldLog, logBackDir, true);
		//                }
		//            }
		//        }
	}

	private static void clearOldJar() {
		File oldJars = JpomManifest.getOldJarsPath();
		List<File> files = FileUtil.loopFiles(oldJars, 1, file -> StrUtil.endWith(file.getName(), FileUtil.JAR_FILE_EXT, true));
		if (CollUtil.isEmpty(files)) {
			return;
		}
		// 排序
		files.sort((o1, o2) -> FileUtil.lastModifiedTime(o2).compareTo(FileUtil.lastModifiedTime(o1)));
		// 截取
		int size = CollUtil.size(files);
		files = CollUtil.sub(files, ExtConfigBean.getInstance().getOldJarsCount(), size);
		// 删除文件
		files.forEach(FileUtil::del);
	}

}
