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

import io.jpom.build.BaseBuildModule;
import io.jpom.model.data.BuildInfoModel;
import io.jpom.model.data.UserModel;
import io.jpom.model.enums.BuildStatus;

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
     * @see BuildInfoModel#getId()
     */
    private String buildDataId;
	/**
	 * 构建名称
	 */
	private String buildName;
    /**
     * 构建编号
     *
     * @see BuildInfoModel#getBuildId()
     */
    private int buildNumberId;
    /**
     * 状态
     *
     * @see BuildStatus
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

	public String getBuildName() {
		return buildName;
	}

	public void setBuildName(String buildName) {
		this.buildName = buildName;
	}
}
