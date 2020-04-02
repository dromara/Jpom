package io.jpom.build;

import cn.hutool.core.io.FileUtil;
import io.jpom.model.BaseModel;
import io.jpom.model.data.BuildModel;

/**
 * 构建物基类
 *
 * @author bwcx_jzy
 * @date 2019/7/19
 */
public class BaseBuildModule extends BaseModel {
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
     * 仅在项目发布类型生效
     *
     * @see io.jpom.model.AfterOpt
     * @see BuildModel#getAfterOpt()
     */
    private int afterOpt;
    /**
     * 是否清空旧包发布
     */
    private boolean clearOld;
    /**
     * 构建产物目录
     */
    private String resultDirFile;
    /**
     * 发布命令  ssh 才能用上
     */
    private String releaseCommand;
    /**
     * 发布到ssh中的目录
     */
    private String releasePath;

    public String getReleasePath() {
        return releasePath;
    }

    public void setReleasePath(String releasePath) {
        this.releasePath = releasePath;
    }

    public String getReleaseCommand() {
        return releaseCommand;
    }

    public void setReleaseCommand(String releaseCommand) {
        this.releaseCommand = releaseCommand;
    }

    public boolean isClearOld() {
        return clearOld;
    }

    public void setClearOld(boolean clearOld) {
        this.clearOld = clearOld;
    }

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
        return FileUtil.normalize(this.resultDirFile.trim());
    }

    public void setResultDirFile(String resultDirFile) {
        this.resultDirFile = resultDirFile;
    }
}
