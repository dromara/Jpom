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
package org.dromara.jpom.build;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.crypto.SecureUtil;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.common.ServerConst;
import org.dromara.jpom.model.data.BuildInfoModel;
import org.dromara.jpom.model.data.RepositoryModel;
import org.springframework.util.Assert;

import java.io.File;
import java.util.function.BiFunction;

/**
 * 构建工具类
 *
 * @author bwcx_jzy
 * @since 2019/7/19
 */
public class BuildUtil {

    public static Long buildCacheSize = 0L;

    /**
     * 刷新存储文件大小
     */
    public static void reloadCacheSize() {
        File buildDataDir = BuildUtil.getBuildDataDir();
        BuildUtil.buildCacheSize = FileUtil.size(buildDataDir);
        //
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
        return FileUtil.file(JpomApplication.getInstance().getDataPath(), "build");
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
        if (buildId <= 0) {
            // 没有 0 号构建id，避免生成 #0 文件夹
            return null;
        }
        if (StrUtil.isEmpty(buildModelId) || StrUtil.isEmpty(resultFile)) {
            return null;
        }
        ResultDirFileAction resultDirFileAction = ResultDirFileAction.parse(resultFile);
        ResultDirFileAction.Type type = resultDirFileAction.getType();
        if (type == ResultDirFileAction.Type.ANT_PATH) {
            // ANT 模式 不能直接获取，避免提前创建文件夹
            return null;
        }
        File result = FileUtil.file(getBuildDataFile(buildModelId), "history", BuildInfoModel.getBuildIdStr(buildId), "result");
        return FileUtil.file(result, resultFile);
    }

    /**
     * 插件构建产物存放路径
     *
     * @param buildModelId 构建实体
     * @param buildId      id
     */
    public static void mkdirHistoryPackageFile(String buildModelId, int buildId) {
        File result = FileUtil.file(getBuildDataFile(buildModelId), "history", BuildInfoModel.getBuildIdStr(buildId), "result");
        FileUtil.mkdir(result);
    }

    /**
     * 获取构建产物存放路径
     *
     * @param buildModelId 构建实体
     * @param buildId      id
     * @return file
     */
    public static File getHistoryPackageZipFile(String buildModelId, int buildId) {
        return FileUtil.file(getBuildDataFile(buildModelId),
                "history",
                BuildInfoModel.getBuildIdStr(buildId),
                "zip");
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
     * 如果为文件夹自动打包为zip ,反之返回null
     *
     * @param file file
     * @return 压缩包文件
     */
    private static File isDirPackage(String id, int buildNumberId, File file) {
        Assert.state(file != null && file.exists(), "产物文件不存在");
        if (file.isFile()) {
            return null;
        }
        Assert.state(!FileUtil.isDirEmpty(file), "文件夹为空,不能打包 #" + buildNumberId);
        String name = FileUtil.getName(file);
        // 如果产物配置 / 时无法获取文件名，采用 result
        name = StrUtil.emptyToDefault(name, "result");
        // 保存目录存放值 history 路径
        File packageFile = BuildUtil.getHistoryPackageZipFile(id, buildNumberId);
        File zipFile = FileUtil.file(packageFile, name + ".zip");
        // 不存在则打包
        ZipUtil.zip(file.getAbsolutePath(), zipFile.getAbsolutePath());
        return zipFile;
    }

    /**
     * 如果为文件夹自动打包为zip ,反之返回null
     *
     * @param file     file
     * @param consumer 文件回调
     * @return 执行结果
     */
    public static <T> T loadDirPackage(String id, int buildNumberId, File file, BiFunction<Boolean, File, T> consumer) {
        File dirPackage = isDirPackage(id, buildNumberId, file);
        if (dirPackage == null) {
            return consumer.apply(false, file);
        } else {
            return consumer.apply(true, dirPackage);
        }
    }


    /**
     * get rsa file
     *
     * @param path 文件名
     * @return file
     */
    public static File getRepositoryRsaFile(String path) {
        File sshDir = FileUtil.file(JpomApplication.getInstance().getDataPath(), ServerConst.SSH_KEY);
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
                rsaFile = FileUtil.file(JpomApplication.getInstance().getTempPath(), ServerConst.SSH_KEY, SecureUtil.sha1(repositoryModel.getGitUrl()) + ServerConst.ID_RSA);
            } else {
                rsaFile = BuildUtil.getRepositoryRsaFile(repositoryModel.getId() + ServerConst.ID_RSA);
            }
            // 写入
            FileUtil.writeUtf8String(repositoryModel.getRsaPrv(), rsaFile);
        }
        Assert.state(FileUtil.isFile(rsaFile), "仓库密钥文件不存在或者异常,请检查后操作");
        return rsaFile;
    }
}
