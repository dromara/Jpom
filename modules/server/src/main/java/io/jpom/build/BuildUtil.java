package io.jpom.build;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import io.jpom.model.data.BuildModel;
import io.jpom.system.ConfigBean;

import java.io.File;

/**
 * 构建工具类
 *
 * @author bwcx_jzy
 * @date 2019/7/19
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

	/**
	 * 获取代码路径
	 *
	 * @param buildModel 实体
	 * @return file
	 */
	public static File getSource(BuildModel buildModel) {
		return FileUtil.file(BuildUtil.getBuildDataFile(buildModel.getId()), "source");
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
		return FileUtil.file(getBuildDataFile(buildModelId),
				"history",
				BuildModel.getBuildIdStr(buildId),
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
		return FileUtil.file(getBuildDataFile(buildModelId),
				"history",
				BuildModel.getBuildIdStr(buildId),
				"info.log");
	}
}
