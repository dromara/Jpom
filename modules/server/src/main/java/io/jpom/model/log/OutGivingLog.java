package io.jpom.model.log;

import io.jpom.model.BaseJsonModel;
import io.jpom.model.data.OutGivingNodeProject;

/**
 * 项目分发日志
 *
 * @author bwcx_jzy
 * @date 2019/7/19
 **/
public class OutGivingLog extends BaseJsonModel {
    /**
     * 表名
     */
    public static final String TABLE_NAME = OutGivingLog.class.getSimpleName().toUpperCase();

    private String id;
    /**
     * 分发id
     */
    private String outGivingId;
    /**
     * 状态
     *
     * @see OutGivingNodeProject.Status
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
     * 处理消息
     */
    private String result;
    /**
     * 节点id
     */
    private String nodeId;
    /**
     * 项目id
     */
    private String projectId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOutGivingId() {
        return outGivingId;
    }

    public void setOutGivingId(String outGivingId) {
        this.outGivingId = outGivingId;
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

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }
}
