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
package io.jpom.model.log;

import io.jpom.model.BaseDbModel;
import io.jpom.service.h2db.TableName;

/**
 * 系统监控记录
 *
 * @author Arno
 * @since 2019/9/16
 */
@TableName(value = "SYSTEMMONITORLOG", name = "节点监控记录")
public class SystemMonitorLog extends BaseDbModel {

	/**
	 * 节点id
	 */
	private String nodeId;
	/**
	 * 监控时间
	 * 插件端返回的时间
	 */
	private Long monitorTime;
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
	 * 网络时间
	 */
	private Integer networkTime;

	public Integer getNetworkTime() {
		return networkTime;
	}

	public void setNetworkTime(Integer networkTime) {
		this.networkTime = networkTime;
	}

	public String getNodeId() {
		return nodeId;
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	public Long getMonitorTime() {
		return monitorTime;
	}

	public void setMonitorTime(Long monitorTime) {
		this.monitorTime = monitorTime;
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
}
