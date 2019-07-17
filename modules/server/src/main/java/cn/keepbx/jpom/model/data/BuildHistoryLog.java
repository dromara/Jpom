package cn.keepbx.jpom.model.data;

import cn.keepbx.jpom.model.BaseJsonModel;

/**
 * 构建历史记录
 *
 * @author bwcx_jzy
 * @date 2019/7/17
 **/
public class BuildHistoryLog extends BaseJsonModel {
    /**
     * 表名
     */
    public static final String TABLE_NAME = BuildHistoryLog.class.getSimpleName().toUpperCase();
    /**
     * 数据id
     */
    private String id;
    /**
     * 关联的构建id
     */
    private String buildDataId;
    /**
     * 构建编号
     */
    private int buildNumberId;
    /**
     * 状态
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
     * 构建产物目录
     */
    private String resultDirFile;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getResultDirFile() {
        return resultDirFile;
    }

    public void setResultDirFile(String resultDirFile) {
        this.resultDirFile = resultDirFile;
    }
}
