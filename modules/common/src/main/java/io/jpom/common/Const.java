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
package io.jpom.common;

/**
 * @author Hotstrip
 * Const class
 */
public class Const {
	/**
	 * String const
	 */
	public static final String ID_STR = "id";
	public static final String GROUP_STR = "group";

	/**
	 * 应用程序类型的配置 key
	 */
	public static final String APPLICATION_NAME = "spring.application.name";

	/**
	 * String get
	 */
	public static final String GET_STR = "get";


	/**
	 * id_rsa
	 */
	public static final String ID_RSA = "_id_rsa";
	/**
	 * sshkey
	 */
	public static final String SSH_KEY = "sshkey";

	/**
	 * SQL backup default directory name
	 * 数据库备份默认目录名称
	 */
	public static final String BACKUP_DIRECTORY_NAME = "backup";
	/**
	 * h2 数据库表名字段
	 */
	public static final String TABLE_NAME = "TABLE_NAME";
	/**
	 * 备份 SQL 文件 后缀
	 */
	public static final String SQL_FILE_SUFFIX = ".sql";
	/**
	 * 升级提示语
	 */
	public static final String UPGRADE_MSG = "升级(重启)中大约需要30秒～2分钟左右";

	/**
	 * 请求 header
	 */
	public static final String WORKSPACEID_REQ_HEADER = "workspaceId";
	/**
	 * 默认的工作空间
	 */
	public static final String WORKSPACE_DEFAULT_ID = "DEFAULT";
	/**
	 * websocket 传输 agent 包 buffer size
	 */
	public static final int DEFAULT_BUFFER_SIZE = 1024 * 1024;
	/**
	 * id 最大长度
	 */
	public static final int ID_MAX_LEN = 50;
}
