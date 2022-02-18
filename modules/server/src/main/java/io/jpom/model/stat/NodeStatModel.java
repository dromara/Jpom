/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.jpom.model.stat;

import cn.hutool.core.util.StrUtil;
import io.jpom.model.BaseGroupModel;
import io.jpom.model.log.SystemMonitorLog;
import io.jpom.service.h2db.TableName;

/**
 * @author bwcx_jzy
 * @see SystemMonitorLog
 * @see io.jpom.model.data.NodeModel
 * @since 2022/1/22
 */
@TableName(value = "NODE_STAT", name = "节点统计")
public class NodeStatModel extends BaseGroupModel {
    /**
     * 占用cpu
     */
    private Double occupyCpu;
    /**
     * 占用内存 （总共）
     */
    private Double occupyMemory;
    /**
     * 占用内存 (使用) @author jzy
     */
    private Double occupyMemoryUsed;
    /**
     * 占用磁盘
     */
    private Double occupyDisk;
    /**
     * 网络耗时
     */
    private Integer networkTime;
    /**
     * 运行时间
     */
    private String upTimeStr;
    /**
     * 系统名称
     */
    private String osName;
    /**
     * jpom 版本
     */
    private String jpomVersion;

    /**
     * 状态{1，无法连接，0 正常, 2 授权信息错误, 3 状态码错误, 4 节点关闭}
     */
    private Integer status;
    /**
     * 错误消息
     */
    private String failureMsg;

    /**
     * 节点地址
     */
    private String url;

    /**
     * 节点名称
     */
    private String name;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFailureMsg() {
        return failureMsg;
    }

    public void setFailureMsg(String failureMsg) {
        this.failureMsg = StrUtil.maxLength(failureMsg, 240);
    }

    public Double getOccupyCpu() {
        return occupyCpu;
    }

    public void setOccupyCpu(Double occupyCpu) {
        this.occupyCpu = occupyCpu;
    }

    public Double getOccupyMemory() {
        return occupyMemory;
    }

    public void setOccupyMemory(Double occupyMemory) {
        this.occupyMemory = occupyMemory;
    }

    public Double getOccupyMemoryUsed() {
        return occupyMemoryUsed;
    }

    public void setOccupyMemoryUsed(Double occupyMemoryUsed) {
        this.occupyMemoryUsed = occupyMemoryUsed;
    }

    public Double getOccupyDisk() {
        return occupyDisk;
    }

    public void setOccupyDisk(Double occupyDisk) {
        this.occupyDisk = occupyDisk;
    }

    public Integer getNetworkTime() {
        return networkTime;
    }

    public void setNetworkTime(Integer networkTime) {
        this.networkTime = networkTime;
    }

    public String getUpTimeStr() {
        return upTimeStr;
    }

    public void setUpTimeStr(String upTimeStr) {
        this.upTimeStr = upTimeStr;
    }

    public String getOsName() {
        return osName;
    }

    public void setOsName(String osName) {
        this.osName = osName;
    }

    public String getJpomVersion() {
        return jpomVersion;
    }

    public void setJpomVersion(String jpomVersion) {
        this.jpomVersion = jpomVersion;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
