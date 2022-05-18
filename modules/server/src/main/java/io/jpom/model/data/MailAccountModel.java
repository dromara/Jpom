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

import cn.hutool.core.util.ObjectUtil;
import io.jpom.model.BaseJsonModel;

/**
 * 系统邮箱配置
 *
 * @author bwcx_jzy
 * @since 2019/7/16
 **/
public class MailAccountModel extends BaseJsonModel {

	public static final String ID = "MAIL_CONFIG";

	/**
	 * SMTP服务器域名
	 */
	private String host;
	/**
	 * SMTP服务端口
	 */
	private Integer port;
	/**
	 * 用户名
	 */
	private String user;
	/**
	 * 密码
	 */
	private String pass;
	/**
	 * 发送方，遵循RFC-822标准
	 */
	private String from;
	/**
	 * 使用 SSL安全连接
	 */
	private Boolean sslEnable;
	/**
	 * 指定的端口连接到在使用指定的套接字工厂。如果没有设置,将使用默认端口
	 */
	@Deprecated
	private Integer socketFactoryPort;

	/**
	 * 超时时间
	 */
	private Integer timeout;

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * 兼容端口
	 *
	 * @return port
	 */
	public Integer getPort() {
		if (sslEnable != null && sslEnable) {
			if (socketFactoryPort != null) {
				return socketFactoryPort;
			}
		}
		return ObjectUtil.defaultIfNull(port, socketFactoryPort);
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

	public String getPass() {
		return pass;
	}

	public void setPass(String pass) {
		this.pass = pass;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public Boolean getSslEnable() {
		return sslEnable;
	}

	public void setSslEnable(Boolean sslEnable) {
		this.sslEnable = sslEnable;
	}

	public Integer getSocketFactoryPort() {
		return socketFactoryPort;
	}

	public void setSocketFactoryPort(Integer socketFactoryPort) {
		this.socketFactoryPort = socketFactoryPort;
	}

	public Integer getTimeout() {
		return timeout;
	}

	public void setTimeout(Integer timeout) {
		this.timeout = timeout;
	}
}
