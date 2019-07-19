package cn.keepbx.jpom.model.vo;

import cn.keepbx.jpom.model.log.BuildHistoryLog;
import cn.keepbx.jpom.model.data.BuildModel;

/**
 * @author bwcx_jzy
 * @date 2019/7/17
 */
public class BuildHistoryLogVo extends BuildHistoryLog {
    private String buildName;

    public String getBuildName() {
        return buildName;
    }

    public void setBuildName(String buildName) {
        this.buildName = buildName;
    }

    public String getBuildIdStr() {
        return BuildModel.getBuildIdStr(getBuildNumberId());
    }
}
