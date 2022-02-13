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
package io.jpom.model.docker;

import cn.hutool.core.annotation.PropIgnore;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import io.jpom.model.BaseWorkspaceModel;
import io.jpom.service.h2db.TableName;
import io.jpom.system.ConfigBean;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Tolerate;
import org.springframework.util.Assert;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author bwcx_jzy
 * @since 2022/1/26
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = "DOCKER_INFO", name = "docker 信息")
@Builder
public class DockerInfoModel extends BaseWorkspaceModel {
	/**
	 * 名称
	 */
	private String name;
	/**
	 * 地址
	 */
	private String host;
	/**
	 * 接口版本
	 */
	private String apiVersion;
	/**
	 * 开启 tls 验证
	 */
	private Boolean tlsVerify;
	/**
	 * 证书路径
	 */
	@PropIgnore
	private Boolean certExist;
	/**
	 * 状态 0 , 异常离线 1 正常
	 */
	private Integer status;
	/**
	 * 错误消息
	 */
	private String failureMsg;
	/**
	 * docker 版本
	 */
	private String dockerVersion;
	/**
	 * 集群节点ID
	 */
	private String swarmNodeId;
	/**
	 * 最后心跳时间
	 */
	private Long lastHeartbeatTime;
	/**
	 * 心跳超时时间，单位 秒
	 */
	private Integer heartbeatTimeout;
	/**
	 * 标签
	 */
	private String tags;
	/**
	 * 集群ID
	 */
	private String swarmId;

	public void setFailureMsg(String failureMsg) {
		this.failureMsg = StrUtil.maxLength(failureMsg, 240);
	}

	@Tolerate
	public DockerInfoModel() {
	}

	/**
	 * 生成证书路径
	 *
	 * @return path
	 */
	public String generateCertPath() {
		String dataPath = ConfigBean.getInstance().getDataPath();
		String host = this.getHost();
		Assert.hasText(host, "host empty");
		host = SecureUtil.sha1(host);
		File docker = FileUtil.file(dataPath, "docker", "tls-cert", host);
		return FileUtil.getAbsolutePath(docker);
	}

	/**
	 * 插件 插件参数 map
	 *
	 * @return 插件需要使用到到参数
	 */
	public Map<String, Object> toParameter() {
		Map<String, Object> parameter = new HashMap<>(10);
		parameter.put("dockerHost", this.getHost());
		parameter.put("apiVersion", this.getApiVersion());
		if (this.getTlsVerify()) {
			parameter.put("dockerCertPath", this.generateCertPath());
		}
		return parameter;
	}

}
