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
package io.jpom.model.data;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import io.jpom.model.*;
import io.jpom.service.h2db.TableName;

import java.util.List;

/**
 * 监控管理实体
 *
 * @author Arno
 */
@TableName("MONITOR_INFO")
public class MonitorModel extends BaseWorkspaceModel {
	private String name;
	/**
	 * 监控的项目
	 */
	private String projects;
	/**
	 * 报警联系人
	 */
	private String notifyUser;
	/**
	 * 异常后是否自动重启
	 */
	private Boolean autoRestart;
	/**
	 * 监控周期
	 */
	private Integer cycle = Cycle.thirty.getCode();

	/**
	 * 监控开启状态
	 */
	private Boolean status;
	/**
	 * 报警状态
	 */
	private Boolean alarm;


	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}


	public Integer getCycle() {
		return cycle;
	}

	public void setCycle(Integer cycle) {
		this.cycle = cycle;
	}

	public Boolean getAutoRestart() {
		return autoRestart != null && autoRestart;
	}

	public void setAutoRestart(Boolean autoRestart) {
		this.autoRestart = autoRestart;
	}

	public Boolean getStatus() {
		return status != null && status;
	}

	public void setStatus(Boolean status) {
		this.status = status;
	}

	public Boolean getAlarm() {
		return alarm != null && alarm;
	}

	public void setAlarm(Boolean alarm) {
		this.alarm = alarm;
	}

	public List<NodeProject> projects() {
		if (StrUtil.isEmpty(projects)) {
			return null;
		}
		return JSONArray.parseArray(projects, NodeProject.class);
	}


	public List<String> notifyUser() {
		if (StrUtil.isEmpty(notifyUser)) {
			return null;
		}
		return JSONArray.parseArray(notifyUser, String.class);
	}

	public String getProjects() {
		return projects;
	}

	public void setProjects(String projects) {
		this.projects = projects;
	}

	public void setProjects(List<NodeProject> projects) {
		if (projects == null) {
			this.projects = null;
		} else {
			this.projects = JSON.toJSONString(projects);
		}
	}

	public String getNotifyUser() {
		return notifyUser;
	}

	public void setNotifyUser(String notifyUser) {
		this.notifyUser = notifyUser;
	}

	public void setNotifyUser(List<String> notifyUser) {
		if (projects == null) {
			this.notifyUser = null;
		} else {
			this.notifyUser = JSON.toJSONString(notifyUser);
		}
	}

	public boolean checkNodeProject(String nodeId, String projectId) {
		List<NodeProject> projects = projects();
		if (projects == null) {
			return false;
		}
		for (NodeProject project : projects) {
			if (project.getNode().equals(nodeId)) {
				List<String> projects1 = project.getProjects();
				if (projects1 == null) {
					return false;
				}
				for (String s : projects1) {
					if (projectId.equals(s)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public enum NotifyType implements BaseEnum {
		/**
		 * 通知方式
		 */
		dingding(0, "钉钉"),
		mail(1, "邮箱"),
		workWx(2, "企业微信"),
//        sms(2, "短信"),
		;

		private final int code;
		private final String desc;

		NotifyType(int code, String desc) {
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

	/**
	 * 通知
	 */
	public static class Notify extends BaseJsonModel {
		private int style;
		private String value;

		public Notify() {
		}

		public Notify(NotifyType style, String value) {
			this.style = style.getCode();
			this.value = value;
		}

		public int getStyle() {
			return style;
		}

		public void setStyle(int style) {
			this.style = style;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}

	public static class NodeProject extends BaseJsonModel {
		private String node;
		private List<String> projects;

		public String getNode() {
			return node;
		}

		public void setNode(String node) {
			this.node = node;
		}

		public List<String> getProjects() {
			return projects;
		}

		public void setProjects(List<String> projects) {
			this.projects = projects;
		}
	}
}
