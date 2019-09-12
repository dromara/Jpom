package io.jpom.model.log;

import io.jpom.model.BaseJsonModel;

/**
 * 系统监控记录
 */
public class SystemMonitorLog extends BaseJsonModel {
    /**
     * 表名
     */
    public static final String TABLE_NAME = SystemMonitorLog.class.getSimpleName().toUpperCase();

    private long id;
    /**
     * 节点id
     */
    private String nodeId;
    /**
     * 监控时间
     */
    private String monitorTime;
    /**
     * 占用cpu
     */
    private double occupyCpu;
    /**
     * 占用内存
     */
    private double occupyMemory;
    /**
     * 占用磁盘
     */
    private double occupyDisk;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getMonitorTime() {
        return monitorTime;
    }

    public void setMonitorTime(String monitorTime) {
        this.monitorTime = monitorTime;
    }

    public double getOccupyCpu() {
        return occupyCpu;
    }

    public void setOccupyCpu(double occupyCpu) {
        this.occupyCpu = occupyCpu;
    }

    public double getOccupyMemory() {
        return occupyMemory;
    }

    public void setOccupyMemory(double occupyMemory) {
        this.occupyMemory = occupyMemory;
    }

    public double getOccupyDisk() {
        return occupyDisk;
    }

    public void setOccupyDisk(double occupyDisk) {
        this.occupyDisk = occupyDisk;
    }

    @Override
    public String toString() {
        return "SystemMonitorLog{" +
                "id=" + id +
                ", nodeId='" + nodeId + '\'' +
                ", monitorTime='" + monitorTime + '\'' +
                ", occupyCpu=" + occupyCpu +
                ", occupyMemory=" + occupyMemory +
                ", occupyDisk=" + occupyDisk +
                '}';
    }
}
