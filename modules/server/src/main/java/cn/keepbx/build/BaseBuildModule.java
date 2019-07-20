package cn.keepbx.build;

import cn.keepbx.jpom.model.BaseJsonModel;
import cn.keepbx.jpom.model.data.BuildModel;

/**
 * 构建物基类
 *
 * @author bwcx_jzy
 * @date 2019/7/19
 */
public class BaseBuildModule extends BaseJsonModel {

    /**
     * 发布方式
     *
     * @see BuildModel.ReleaseMethod
     * @see BuildModel#getReleaseMethod()
     */
    private int releaseMethod;
    /**
     * 发布方法的数据id
     *
     * @see BuildModel#getReleaseMethodDataId()
     */
    private String releaseMethodDataId;
    /**
     * 分发后的操作
     *
     * @see BuildModel#getAfterOpt()
     */
    private int afterOpt;
    /**
     * 构建产物目录
     */
    private String resultDirFile;

    public int getReleaseMethod() {
        return releaseMethod;
    }

    public void setReleaseMethod(int releaseMethod) {
        this.releaseMethod = releaseMethod;
    }

    public String getReleaseMethodDataId() {
        return releaseMethodDataId;
    }

    public void setReleaseMethodDataId(String releaseMethodDataId) {
        this.releaseMethodDataId = releaseMethodDataId;
    }

    public int getAfterOpt() {
        return afterOpt;
    }

    public void setAfterOpt(int afterOpt) {
        this.afterOpt = afterOpt;
    }

    public String getResultDirFile() {
        if (resultDirFile == null) {
            return null;
        }
        return resultDirFile.trim();
    }

    public void setResultDirFile(String resultDirFile) {
        this.resultDirFile = resultDirFile;
    }
}
