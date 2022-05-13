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
package io.jpom.build;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.crypto.SecureUtil;
import io.jpom.common.Const;
import io.jpom.model.data.BuildInfoModel;
import io.jpom.model.data.RepositoryModel;
import io.jpom.system.ConfigBean;
import org.springframework.util.Assert;

import java.io.File;

/**
 * 构建工具类
 *
 * @author bwcx_jzy
 * @since 2019/7/19
 */
public class BuildUtil {

	public static Long buildCacheSize = 0L;
	public static long tempFileCacheSize = 0L;

	/**
	 * 刷新存储文件大小
	 */
	public static void reloadCacheSize() {
		File buildDataDir = BuildUtil.getBuildDataDir();
		BuildUtil.buildCacheSize = FileUtil.size(buildDataDir);
		//
		File file = ConfigBean.getInstance().getTempPath();
		tempFileCacheSize = FileUtil.size(file);
	}

	public static File getBuildDataFile(String id) {
		return FileUtil.file(getBuildDataDir(), id);
	}

//	/**
//	 * 获取代码路径
//	 *
//	 * @param buildModel 实体
//	 * @return file
//	 * @see BuildUtil#getSourceById
//	 */
//	@Deprecated
//	public static File getSource(BuildModel buildModel) {
//		return FileUtil.file(BuildUtil.getBuildDataFile(buildModel.getId()), "source");
//	}

	/**
	 * @param id 构建ID
	 * @return file
	 * @author Hotstrip
	 * 新版本获取代码路径
	 * @since 2021-08-22
	 */
	public static File getSourceById(String id) {
		return FileUtil.file(BuildUtil.getBuildDataFile(id), "source");
	}

	public static File getBuildDataDir() {
		return FileUtil.file(ConfigBean.getInstance().getDataPath(), "build");
	}

	/**
	 * 获取构建产物存放路径
	 *
	 * @param buildModelId 构建实体
	 * @param buildId      id
	 * @param resultFile   结果目录
	 * @return file
	 */
	public static File getHistoryPackageFile(String buildModelId, int buildId, String resultFile) {
		if (StrUtil.isEmpty(buildModelId) || StrUtil.isEmpty(resultFile)) {
			return null;
		}
		return FileUtil.file(getBuildDataFile(buildModelId),
				"history",
				BuildInfoModel.getBuildIdStr(buildId),
				"result", resultFile);
	}

	/**
	 * 如果为文件夹自动打包为zip ,反之返回null
	 *
	 * @param file file
	 * @return 压缩包文件
	 */
	public static File isDirPackage(File file) {
		if (file.isFile()) {
			return null;
		}
		String name = FileUtil.getName(file);
		if (StrUtil.isEmpty(name)) {
			name = "result";
		}
		File zipFile = FileUtil.file(file.getParentFile().getParentFile(), name + ".zip");
		if (!zipFile.exists()) {
			// 不存在则打包
			ZipUtil.zip(file.getAbsolutePath(), zipFile.getAbsolutePath());
		}
		return zipFile;
	}

	/**
	 * 获取日志记录文件
	 *
	 * @param buildModelId buildModelId
	 * @param buildId      构建编号
	 * @return file
	 */
	public static File getLogFile(String buildModelId, int buildId) {
		if (StrUtil.isEmpty(buildModelId)) {
			return null;
		}
		return FileUtil.file(getBuildDataFile(buildModelId),
				"history",
				BuildInfoModel.getBuildIdStr(buildId),
				"info.log");
	}

	/**
	 * get rsa file
	 *
	 * @param path 文件名
	 * @return file
	 */
	public static File getRepositoryRsaFile(String path) {
		File sshDir = FileUtil.file(ConfigBean.getInstance().getDataPath(), Const.SSH_KEY);
		return FileUtil.file(sshDir, path);
	}

	/**
	 * get rsa file
	 *
	 * @param repositoryModel 仓库
	 * @return 文件
	 */
	public static File getRepositoryRsaFile(RepositoryModel repositoryModel) {
		if (StrUtil.isEmpty(repositoryModel.getRsaPrv())) {
			return null;
		}
		// ssh
		File rsaFile;
		if (StrUtil.startWith(repositoryModel.getRsaPrv(), URLUtil.FILE_URL_PREFIX)) {
			String rsaPath = StrUtil.removePrefix(repositoryModel.getRsaPrv(), URLUtil.FILE_URL_PREFIX);
			rsaFile = FileUtil.file(rsaPath);
		} else {
			if (StrUtil.isEmpty(repositoryModel.getId())) {
				rsaFile = FileUtil.file(ConfigBean.getInstance().getTempPath(), Const.SSH_KEY, SecureUtil.sha1(repositoryModel.getGitUrl()) + Const.ID_RSA);
			} else {
				rsaFile = BuildUtil.getRepositoryRsaFile(repositoryModel.getId() + Const.ID_RSA);
			}
			// 写入
			FileUtil.writeUtf8String(repositoryModel.getRsaPrv(), rsaFile);
		}
		Assert.state(FileUtil.isFile(rsaFile), "仓库密钥文件不存在或者异常,请检查后操作");
		return rsaFile;
	}
}
