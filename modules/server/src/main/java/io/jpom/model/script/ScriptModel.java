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
package io.jpom.model.script;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import io.jpom.model.BaseWorkspaceModel;
import io.jpom.service.h2db.TableName;
import io.jpom.system.ConfigBean;
import io.jpom.system.ExtConfigBean;
import io.jpom.util.CommandUtil;

import java.io.File;

/**
 * @author bwcx_jzy
 * @since 2022/1/19
 */
@TableName(value = "SERVER_SCRIPT_INFO", name = "脚本模版")
public class ScriptModel extends BaseWorkspaceModel {
	/**
	 * 模版名称
	 */
	private String name;
	/**
	 * 最后执行人员
	 */
	private String lastRunUser;
	/**
	 * 定时执行
	 */
	private String autoExecCron;
	/**
	 * 默认参数
	 */
	private String defArgs;
	/**
	 * 描述
	 */
	private String description;

	private String context;
	/**
	 * 节点ID
	 */
	private String nodeIds;

	public String getNodeIds() {
		return nodeIds;
	}

	public void setNodeIds(String nodeIds) {
		this.nodeIds = nodeIds;
	}

	public String getContext() {
		return context;
	}

	public void setContext(String context) {
		this.context = context;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getLastRunUser() {
		return lastRunUser;
	}

	public void setLastRunUser(String lastRunUser) {
		this.lastRunUser = lastRunUser;
	}

	public String getAutoExecCron() {
		return autoExecCron;
	}

	public void setAutoExecCron(String autoExecCron) {
		this.autoExecCron = autoExecCron;
	}

	public String getDefArgs() {
		return defArgs;
	}

	public void setDefArgs(String defArgs) {
		this.defArgs = defArgs;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}


	public File scriptPath() {
		if (StrUtil.isEmpty(getId())) {
			throw new IllegalArgumentException("id 为空");
		}
		File path = ConfigBean.getInstance().getScriptPath();
		return FileUtil.file(path, getId());
	}

	public File logFile(String executeId) {
		File path = this.scriptPath();
		return FileUtil.file(path, "log", executeId + ".log");
	}

	public File scriptFile() {
		File path = this.scriptPath();
		File file = FileUtil.file(path, StrUtil.format("script.{}", CommandUtil.SUFFIX));
		//
		FileUtil.writeString(getContext(), file, ExtConfigBean.getInstance().getConsoleLogCharset());
		return file;
	}
}
