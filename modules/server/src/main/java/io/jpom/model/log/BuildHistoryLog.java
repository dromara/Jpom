package io.jpom.model.log;

import io.jpom.build.BaseBuildModule;
import io.jpom.model.data.BuildModel;
import io.jpom.model.data.UserModel;

/**
 * 构建历史记录
 *
 * @author bwcx_jzy
 * @date 2019/7/17
 **/
public class BuildHistoryLog extends BaseBuildModule {
    /**
     * 表名
     */
    public static final String TABLE_NAME = BuildHistoryLog.class.getSimpleName().toUpperCase();

    /**
     * 关联的构建id
     *
     * @see BuildModel#getId()
     */
    private String buildDataId;
    /**
     * 构建编号
     *
     * @see BuildModel#getBuildId()
     */
    private int buildNumberId;
    /**
     * 状态
     *
     * @see BuildModel.Status
     */
    private int status;
    /**
     * 开始时间
     */
    private long startTime;
    /**
     * 结束时间
     */
    private long endTime;


    /**
     * 构建人
     *
     * @see UserModel#getName()
     */
    private String buildUser;

    public String getBuildUser() {
        return buildUser;
    }

    public void setBuildUser(String buildUser) {
        this.buildUser = buildUser;
    }

    public String getBuildDataId() {
        return buildDataId;
    }

    public void setBuildDataId(String buildDataId) {
        this.buildDataId = buildDataId;
    }

    public int getBuildNumberId() {
        return buildNumberId;
    }

    public void setBuildNumberId(int buildNumberId) {
        this.buildNumberId = buildNumberId;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }
}
