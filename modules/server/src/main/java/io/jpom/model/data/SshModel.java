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

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import io.jpom.model.BaseWorkspaceModel;
import io.jpom.service.h2db.TableName;
import io.jpom.util.StringUtil;

import java.nio.charset.Charset;
import java.util.List;

/**
 * ssh 信息
 *
 * @author bwcx_jzy
 * @date 2019/8/9
 */
@TableName(value = "SSH_INFO", name = "SSH 信息")
public class SshModel extends BaseWorkspaceModel {

	private String name;
	private String host;
	private Integer port;
	private String user;
	private String password;
	/**
	 * 编码格式
	 */
	private String charset;

	/**
	 * 文件目录
	 */
	private String fileDirs;

	/**
	 * ssh 私钥
	 */
	private String privateKey;

	private String connectType;

	/**
	 * 不允许执行的命令
	 */
	private String notAllowedCommand;

	/**
	 * 允许编辑的后缀文件
	 */
	private String allowEditSuffix;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNotAllowedCommand() {
		return notAllowedCommand;
	}

	public void setNotAllowedCommand(String notAllowedCommand) {
		this.notAllowedCommand = notAllowedCommand;
	}

	public ConnectType connectType() {
		return EnumUtil.fromString(ConnectType.class, this.connectType, ConnectType.PASS);
	}

	public String getConnectType() {
		return connectType;
	}

	public void setConnectType(String connectType) {
		this.connectType = connectType;
	}

	public String getPrivateKey() {
		return privateKey;
	}

	public void setPrivateKey(String privateKey) {
		this.privateKey = privateKey;
	}

	public String getFileDirs() {
		return fileDirs;
	}

	public void setFileDirs(String fileDirs) {
		this.fileDirs = fileDirs;
	}

	public List<String> fileDirs() {
		return StringUtil.jsonConvertArray(this.fileDirs, String.class);
	}

	public void fileDirs(List<String> fileDirs) {
		if (fileDirs != null) {
			for (int i = fileDirs.size() - 1; i >= 0; i--) {
				String s = fileDirs.get(i);
				fileDirs.set(i, FileUtil.normalize(s));
			}
			this.fileDirs = JSONArray.toJSONString(fileDirs);
		} else {
			this.fileDirs = null;
		}
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getCharset() {
		return charset;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	public Charset getCharsetT() {
		Charset charset;
		try {
			charset = Charset.forName(this.getCharset());
		} catch (Exception e) {
			charset = CharsetUtil.CHARSET_UTF_8;
		}
		return charset;
	}

	public List<String> allowEditSuffix() {
		return StringUtil.jsonConvertArray(this.allowEditSuffix, String.class);
	}

	public void allowEditSuffix(List<String> allowEditSuffix) {
		if (allowEditSuffix == null) {
			this.allowEditSuffix = null;
		} else {
			this.allowEditSuffix = JSONArray.toJSONString(allowEditSuffix);
		}
	}

	public String getAllowEditSuffix() {
		return allowEditSuffix;
	}

	public void setAllowEditSuffix(String allowEditSuffix) {
		this.allowEditSuffix = allowEditSuffix;
	}

	/**
	 * 检查是否包含禁止命令
	 *
	 * @param sshItem   实体
	 * @param inputItem 输入的命令
	 * @return false 存在禁止输入的命令
	 */
	public static boolean checkInputItem(SshModel sshItem, String inputItem) {
		// 检查禁止执行的命令
		String notAllowedCommand = StrUtil.emptyToDefault(sshItem.getNotAllowedCommand(), StrUtil.EMPTY).toLowerCase();
		if (StrUtil.isEmpty(notAllowedCommand)) {
			return true;
		}
		List<String> split = StrUtil.split(notAllowedCommand, StrUtil.COMMA);
		inputItem = inputItem.toLowerCase();
		List<String> commands = StrUtil.split(inputItem, StrUtil.CR);
		commands.addAll(StrUtil.split(inputItem, "&"));
		for (String s : split) {
			//
			boolean anyMatch = commands.stream().anyMatch(item -> StrUtil.startWithAny(item, s + StrUtil.SPACE, ("&" + s + StrUtil.SPACE), StrUtil.SPACE + s + StrUtil.SPACE));
			if (anyMatch) {
				return false;
			}
			//
			anyMatch = commands.stream().anyMatch(item -> StrUtil.equals(item, s));
			if (anyMatch) {
				return false;
			}
		}
		return true;
	}

	public enum ConnectType {
		/**
		 * 账号密码
		 */
		PASS,
		/**
		 * 密钥
		 */
		PUBKEY
	}
}
