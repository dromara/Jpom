package cn.keepbx.jpom.model.vo;

import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.build.BuildUtil;
import cn.keepbx.jpom.model.BaseEnum;
import cn.keepbx.jpom.model.data.BuildModel;
import cn.keepbx.jpom.model.data.OutGivingModel;
import cn.keepbx.jpom.model.log.BuildHistoryLog;
import cn.keepbx.jpom.service.node.OutGivingServer;

import java.io.File;
import java.io.IOException;

/**
 * 构建产物vo
 *
 * @author bwcx_jzy
 * @date 2019/7/17
 */
public class BuildHistoryLogVo extends BuildHistoryLog {
    private String buildName;
    private String releaseDesc;
    /**
     * 是否存在构建产物
     */
    private boolean hashFile;
    /**
     * 是否存在日志
     */
    private boolean hasLog;

    public boolean isHasLog() {
        File file = BuildUtil.getLogFile(getBuildDataId(), getBuildNumberId());
        hasLog = file.exists();
        return hasLog;
    }

    public void setHasLog(boolean hasLog) {
        this.hasLog = hasLog;
    }

    public String getBuildName() {
        return buildName;
    }

    public void setBuildName(String buildName) {
        this.buildName = buildName;
    }

    public boolean isHashFile() {
        File file = BuildUtil.getHistoryPackageFile(getBuildDataId(), getBuildNumberId(), getResultDirFile());
        hashFile = file.exists();
        return hashFile;
    }

    public void setHashFile(boolean hashFile) {
        this.hashFile = hashFile;
    }

    public String getBuildIdStr() {
        return BuildModel.getBuildIdStr(getBuildNumberId());
    }

    public void setReleaseDesc(String releaseDesc) {
        this.releaseDesc = releaseDesc;
    }

    /**
     * 发布描述
     *
     * @return 描述
     */
    public String getReleaseDesc() {
        if (releaseDesc == null) {
            int releaseMethod = getReleaseMethod();
            BuildModel.ReleaseMethod releaseMethod1 = BaseEnum.getEnum(BuildModel.ReleaseMethod.class, releaseMethod);
            if (releaseMethod1 == null) {
                return BuildModel.ReleaseMethod.No.getDesc();
            }
            String releaseMethodDataId = getReleaseMethodDataId();
            switch (releaseMethod1) {
                case Project: {
                    String[] datas = releaseMethodDataId.split(":");
                    return String.format("【%s】节点【%s】项目", datas[0], datas[1]);
                }
                case Outgiving: {
                    OutGivingServer outGivingServer = SpringUtil.getBean(OutGivingServer.class);
                    OutGivingModel item = outGivingServer.getItem(releaseMethodDataId);
                    if (item == null) {
                        return "-";
                    }
                    return "【" + item.getName() + "】分发";
                }
                case No:
                default:
                    return releaseMethod1.getDesc();
            }
        }
        return releaseDesc;
    }

}
