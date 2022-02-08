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
package io.jpom.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author lf
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AgentFileModel extends BaseModel {

	/**
	 * 保存Agent文件
	 */
	public static final String ID = "AGENT_FILE";
	/**
	 * 最新插件端包的文件名
	 */
	public static final String ZIP_NAME = "agent.zip";
	/**
	 * 默认空版本信息
	 */
	public static final AgentFileModel EMPTY = new AgentFileModel();
	/**
	 * 文件大小
	 */
	private Long size = 0L;
	/**
	 * 保存路径
	 */
	private String savePath;
	/**
	 * 版本号
	 */
	private String version;
	/**
	 * jar 打包时间
	 */
	private String timeStamp;

	@Override
	public String toString() {
		return super.toString();
	}
}
