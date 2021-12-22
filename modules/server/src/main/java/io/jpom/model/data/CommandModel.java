package io.jpom.model.data;

import io.jpom.model.BaseJsonModel;
import io.jpom.model.BaseWorkspaceModel;
import io.jpom.service.h2db.TableName;
import io.jpom.util.StringUtil;

import java.util.List;

/**
 * @author : Arno
 * 指令信息
 * @since : 2021/12/4 18:38
 */
@TableName(value = "COMMAND_MODEL", name = "命令管理")
public class CommandModel extends BaseWorkspaceModel {
	/**
	 * 命令名称
	 */
	private String name;
	/**
	 * 命令描述
	 */
	private String desc;
	/**
	 * 指令内容
	 */
	private String command;
	/**
	 * 执行用户，默认为root
	 */
	private String executionRole;
	/**
	 * 执行路径，默认为~/
	 */
	private String executionPath;
	/**
	 * 命令类型，0-shell，1-powershell
	 */
	private Integer type;
	/**
	 * 命令参数
	 */
	private String params;
	/**
	 * 超时时间,单位：秒，默认60
	 */
	private Integer timeout;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public String getExecutionRole() {
		return executionRole;
	}

	public void setExecutionRole(String executionRole) {
		this.executionRole = executionRole;
	}

	public String getExecutionPath() {
		return executionPath;
	}

	public void setExecutionPath(String executionPath) {
		this.executionPath = executionPath;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(Integer timeout) {
		this.timeout = timeout;
	}

	public Integer getType() {
		return type;
	}

	public void setType(Integer type) {
		this.type = type;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public String getParams() {
		return params;
	}

	public void setParams(String params) {
		this.params = params;
	}

	public List<CommandParam> params() {
		return StringUtil.jsonConvertArray(params, CommandParam.class);
	}

	public static class CommandParam extends BaseJsonModel {
		/**
		 * 参数名
		 */
		private String name;
		/**
		 * 参数值
		 */
		private String val;
		/**
		 * 描述
		 */
		private String desc;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getVal() {
			return val;
		}

		public void setVal(String val) {
			this.val = val;
		}

		public String getDesc() {
			return desc;
		}

		public void setDesc(String desc) {
			this.desc = desc;
		}
	}
}
