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

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import io.jpom.JpomApplication;
import io.jpom.system.AgentConfigBean;
import io.jpom.system.ExtConfigBean;
import io.jpom.util.CommandUtil;

import java.io.File;

/**
 * 脚本模板
 *
 * @author jiangzeyin
 * @date 2019/4/24
 */
public class NodeScriptModel extends BaseWorkspaceModel {

	/**
	 * 最后执行人员
	 */
	private String lastRunUser;
	/**
	 * 最后修改时间
	 */
	private String modifyTime;
	/**
	 * 脚本内容
	 */
	private String context;

	public String getLastRunUser() {
		return StrUtil.emptyToDefault(lastRunUser, StrUtil.DASHED);
	}

	public void setLastRunUser(String lastRunUser) {
		this.lastRunUser = lastRunUser;
	}

	public String getModifyTime() {
		return modifyTime;
	}

	public void setModifyTime(String modifyTime) {
		this.modifyTime = modifyTime;
	}

	public String getContext() {
		return context;
	}

	public void setContext(String context) {
		this.context = context;
	}

	public File getFile(boolean get) {
		if (StrUtil.isEmpty(getId())) {
			throw new IllegalArgumentException("id 为空");
		}
		File path = AgentConfigBean.getInstance().getScriptPath();
		return FileUtil.file(path, getId(), "script." + CommandUtil.SUFFIX);
	}

	public File logFile(String executeId) {
		if (StrUtil.isEmpty(getId())) {
			throw new IllegalArgumentException("id 为空");
		}
		File path = AgentConfigBean.getInstance().getScriptPath();
		return FileUtil.file(path, getId(), "log", executeId + ".log");
	}

	public void saveFile() {
		File file = getFile(true);
		FileUtil.writeString(getContext(), file, ExtConfigBean.getInstance().getConsoleLogCharset());
//        // 添加权限
//        if (SystemUtil.getOsInfo().isLinux()) {
//            CommandUtil.execCommand("chmod 755 " + FileUtil.getAbsolutePath(file));
//        }
	}

	/**
	 * 读取文件信息
	 */
	public void readFileTime() {
		File file = getFile(true);
		long lastModified = file.lastModified();
		setModifyTime(DateUtil.date(lastModified).toString());

	}

	public void readFileContext() {
		File file = getFile(true);
		if (FileUtil.exist(file)) {
			//
			String context = FileUtil.readString(file, ExtConfigBean.getInstance().getConsoleLogCharset());
			setContext(context);
		}
	}
}
