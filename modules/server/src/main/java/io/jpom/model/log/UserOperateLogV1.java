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

import cn.hutool.core.text.CharPool;
import cn.hutool.core.util.StrUtil;
import io.jpom.model.BaseEnum;
import io.jpom.model.BaseWorkspaceModel;
import io.jpom.service.h2db.TableName;

/**
 * 用户操作日志
 *
 * @author jiangzeyin
 * @date 2019/4/19
 */
@TableName(value = "USEROPERATELOGV1", name = "用户操作日志")
public class UserOperateLogV1 extends BaseWorkspaceModel {
	/**
	 * 操作ip
	 */
	private String ip;
	/**
	 * 用户ip
	 */
	private String userId;
	/**
	 * 节点id
	 */
	private String nodeId;
	/**
	 * 操作时间
	 */
	private Long optTime;
	/**
	 * 操作状态
	 */
	private Integer optStatus = Status.Fail.getCode();
	/**
	 * 完整消息
	 */
	private String resultMsg;
	/**
	 * 操作id
	 * 用于socket 回话回调更新
	 */
	@Deprecated
	private String reqId;
	/**
	 * 请求参数
	 */
	private String reqData;
	/**
	 * 数据id
	 */
	private String dataId;
	/**
	 * 浏览器标识
	 */
	private String userAgent;

	private String classFeature;
	private String methodFeature;

	public String getClassFeature() {
		return classFeature;
	}

	public void setClassFeature(String classFeature) {
		this.classFeature = classFeature;
	}

	public String getMethodFeature() {
		return methodFeature;
	}

	public void setMethodFeature(String methodFeature) {
		this.methodFeature = methodFeature;
	}

	public String getReqData() {
		return reqData;
	}

	public void setReqData(String reqData) {
		this.reqData = StrUtil.maxLength(reqData, 999999990);
	}

	public String getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = StrUtil.maxLength(userAgent, 280);
	}

	public String getDataId() {
		return StrUtil.emptyToDefault(dataId, StrUtil.DASHED);
	}

	public void setDataId(String dataId) {
		this.dataId = dataId;
	}

	@Override
	public void setId(String id) {
		super.setId(id);
		this.setReqId(id);
	}

	//	public UserOperateLogV1(String reqId) {
//		if (reqId == null) {
//			this.reqId = IdUtil.fastUUID();
//		} else {
//			this.reqId = reqId;
//		}
//	}

//	/**
//	 * 操作id
//	 */
//	public UserOperateLogV1() {
//	}

	public String getReqId() {
		return reqId;
	}

	public void setReqId(String reqId) {
		this.reqId = reqId;
	}

	public String getNodeId() {
		return StrUtil.emptyToDefault(nodeId, StrUtil.DASHED);
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public Long getOptTime() {
		return optTime;
	}

	public void setOptTime(Long optTime) {
		this.optTime = optTime;
	}

	public int getOptStatus() {
		return optStatus;
	}

	/**
	 * 获取执行结果的描述消息
	 *
	 * @return 成功/ 失败：状态码
	 */
	public String getOptStatusMsg() {
		if (getOptStatus() == Status.Success.getCode()) {
			return Status.Success.getDesc();
		}
		return Status.Fail.getDesc() + CharPool.COLON + getOptStatus();
	}

	public void setOptStatus(int optStatus) {
		this.optStatus = optStatus;
	}

	public String getResultMsg() {
		return resultMsg;
	}

	public void setResultMsg(String resultMsg) {
		this.resultMsg = StrUtil.maxLength(resultMsg, 999999990);
	}

	/**
	 * 状态状态
	 */
	public enum Status implements BaseEnum {
		/**
		 * 请求状态码200 为成功
		 */
		Success(200, "成功"),
		Fail(0, "失败");
		private final int code;
		private final String desc;

		@Override
		public int getCode() {
			return code;
		}

		@Override
		public String getDesc() {
			return desc;
		}


		Status(int code, String desc) {
			this.code = code;
			this.desc = desc;
		}
	}
}
