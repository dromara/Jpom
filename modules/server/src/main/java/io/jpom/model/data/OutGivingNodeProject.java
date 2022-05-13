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
package io.jpom.model.data;

import cn.hutool.core.util.StrUtil;
import io.jpom.model.BaseEnum;
import io.jpom.model.BaseJsonModel;

/**
 * 节点项目
 *
 * @author jiangzeyin
 * @since 2019/4/22
 */
public class OutGivingNodeProject extends BaseJsonModel {

	private String nodeId;
	private String projectId;
	private String lastOutGivingTime;
	private Integer status;
	private String result;


	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	public Integer getStatus() {
		return status;
	}

	public String getStatusMsg() {
		return BaseEnum.getDescByCode(Status.class, getStatus());
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public String getLastOutGivingTime() {
		return StrUtil.emptyToDefault(lastOutGivingTime, StrUtil.DASHED);
	}

	public void setLastOutGivingTime(String lastOutGivingTime) {
		this.lastOutGivingTime = lastOutGivingTime;
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

	/**
	 * 状态
	 */
	public enum Status implements BaseEnum {
		/**
		 *
		 */
		No(0, "未分发"),
		Ing(1, "分发中"),
		Ok(2, "分发成功"),
		Fail(3, "分发失败"),
		Cancel(4, "取消分发"),
		;
		private final int code;
		private final String desc;

		Status(int code, String desc) {
			this.code = code;
			this.desc = desc;
		}

		@Override
		public int getCode() {
			return code;
		}

		@Override
		public String getDesc() {
			return desc;
		}
	}
}
