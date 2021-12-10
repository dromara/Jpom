/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 码之科技工作室
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
 * @date 2019/9/16
 */
@TableName(value = "SYSTEMMONITORLOG", name = "节点监控记录")
public class SystemMonitorLog extends BaseDbModel {

	/**
	 * 节点id
	 */
	private String nodeId;
	/**
	 * 监控时间
	 */
	private long monitorTime;
	/**
	 * 占用cpu
	 */
	private double occupyCpu;
	/**
	 * 占用内存 （总共）
	 */
	private double occupyMemory;
	/**
	 * 占用内存 (使用) @author jzy
	 */
	private double occupyMemoryUsed;
	/**
	 * 占用磁盘
	 */
	private double occupyDisk;

	public String getNodeId() {
		return nodeId;
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	public long getMonitorTime() {
		return monitorTime;
	}

	public void setMonitorTime(long monitorTime) {
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

	public double getOccupyMemoryUsed() {
		return occupyMemoryUsed;
	}

	public void setOccupyMemoryUsed(double occupyMemoryUsed) {
		this.occupyMemoryUsed = occupyMemoryUsed;
	}
}
