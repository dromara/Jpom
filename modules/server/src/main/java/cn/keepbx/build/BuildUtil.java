package cn.keepbx.build;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ZipUtil;
import cn.keepbx.jpom.model.data.BuildModel;
import cn.keepbx.jpom.system.ConfigBean;

import java.io.File;

/**
 * 构建工具类
 *
 * @author bwcx_jzy
 * @date 2019/7/19
 */
public class BuildUtil {

    public static File getBuildDataFile(String id) {
        return FileUtil.file(ConfigBean.getInstance().getDataPath(), "build", id);
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
                resultFile, "result");
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
        File zipFile = FileUtil.file(file.getParentFile(), FileUtil.getName(file) + ".zip");
        if (!zipFile.exists()) {
            // 不存在则打包
            ZipUtil.zip(file);
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
