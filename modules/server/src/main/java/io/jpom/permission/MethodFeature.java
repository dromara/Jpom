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
package io.jpom.permission;

/**
 * 功能方法
 *
 * @author bwcx_jzy
 * @since 2019/8/13
 */
public enum MethodFeature {
	/**
	 * 没有
	 */
	NULL(""),
	/**
	 * 文件管理
	 */
//	FILE("文件管理"),
	EDIT("修改、添加数据"),
	DEL("删除数据"),
	//	INSTALL("安装"),
	LIST("列表、查询"),
	//	TERMINAL("终端"),
	DOWNLOAD("下载"),
	//	LOG("日志"),
	UPLOAD("上传"),
	//    WHITELIST("白名单"),
	EXECUTE("执行"),
	//	DEL_FILE("删除文件"),
	//	CACHE("缓存"),
//	DEL_LOG("删除日志"),
//	CONFIG("配置"),
//	READ_FILE("读取文件"),
	//	GET_FILE_FOMAT("获取在线编辑文件格式"),
//	UPDATE_CONFIG_FILE("更新文件"),
	REMOTE_DOWNLOAD("下载远程文件"),
	;

	private final String name;

	public String getName() {
		return name;
	}

	MethodFeature(String name) {
		this.name = name;
	}
}
