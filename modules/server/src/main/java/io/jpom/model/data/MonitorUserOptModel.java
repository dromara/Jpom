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

import com.alibaba.fastjson.JSON;
import io.jpom.model.BaseWorkspaceModel;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.MethodFeature;
import io.jpom.service.h2db.TableName;
import io.jpom.util.StringUtil;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 监控用户操作实体
 *
 * @author Arno
 */
@TableName(value = "MONITOR_USER_OPT",name = "监控用户操作")
public class MonitorUserOptModel extends BaseWorkspaceModel {
	/**
	 *
	 */
	private String name;
	/**
	 * 监控的人员
	 */
	private String monitorUser;
	/**
	 * 监控的功能
	 *
	 * @see ClassFeature
	 */
	private String monitorFeature;
	/**
	 * 监控的操作
	 *
	 * @see MethodFeature
	 */
	private String monitorOpt;
	/**
	 * 报警联系人
	 */
	private String notifyUser;

	/**
	 * 监控开启状态
	 */
	private Boolean status;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setMonitorUser(String monitorUser) {
		this.monitorUser = monitorUser;
	}

	public String getMonitorFeature() {
		List<ClassFeature> object = monitorFeature();
		return object == null ? null : JSON.toJSONString(object);
	}

	public List<ClassFeature> monitorFeature() {
		return StringUtil.jsonConvertArray(monitorFeature, ClassFeature.class);
	}

	public void setMonitorFeature(String monitorFeature) {
		this.monitorFeature = monitorFeature;
	}

	public void monitorFeature(List<ClassFeature> monitorFeature) {
		if (monitorFeature == null) {
			this.monitorFeature = null;
		} else {
			this.monitorFeature = JSON.toJSONString(monitorFeature.stream().map(Enum::name).collect(Collectors.toList()));
		}
	}

	public String getMonitorOpt() {
		List<MethodFeature> object = monitorOpt();
		return object == null ? null : JSON.toJSONString(object);
	}


	public List<MethodFeature> monitorOpt() {
		return StringUtil.jsonConvertArray(monitorOpt, MethodFeature.class);
	}

	public void setMonitorOpt(String monitorOpt) {
		this.monitorOpt = monitorOpt;
	}

	public void monitorOpt(List<MethodFeature> monitorOpt) {
		if (monitorOpt == null) {
			this.monitorOpt = null;
		} else {
			this.monitorOpt = JSON.toJSONString(monitorOpt.stream().map(Enum::name).collect(Collectors.toList()));
		}
	}

	public void setNotifyUser(String notifyUser) {
		this.notifyUser = notifyUser;
	}

	public Boolean getStatus() {
		return status;
	}

	public String getMonitorUser() {
		List<String> object = monitorUser();
		return object == null ? null : JSON.toJSONString(object);
	}

	public String getNotifyUser() {
		List<String> object = notifyUser();
		return object == null ? null : JSON.toJSONString(object);

	}

	public List<String> monitorUser() {
		return StringUtil.jsonConvertArray(monitorUser, String.class);
	}

	public void monitorUser(List<String> monitorUser) {
		if (monitorUser == null) {
			this.monitorUser = null;
		} else {
			this.monitorUser = JSON.toJSONString(monitorUser);
		}
	}

	public List<String> notifyUser() {
		return StringUtil.jsonConvertArray(notifyUser, String.class);
	}

	public void notifyUser(List<String> notifyUser) {
		if (notifyUser == null) {
			this.notifyUser = null;
		} else {
			this.notifyUser = JSON.toJSONString(notifyUser);
		}
	}


	public boolean isStatus() {
		return status != null && status;
	}

	public void setStatus(Boolean status) {
		this.status = status;
	}
}
