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
package io.jpom;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;

import java.time.Duration;
import java.util.Map;

/**
 * @author bwcx_jzy
 * @since 2022/1/26
 */
public class DockerUtil {

	/**
	 * 构建 docker client 对象
	 *
	 * @param parameter      参数
	 * @param maxConnections 最新连接
	 * @return DockerClient
	 */
	public static DockerClient build(Map<String, Object> parameter, int maxConnections) {
		String host = (String) parameter.get("dockerHost");
		String apiVersion = (String) parameter.get("apiVersion");
		String dockerCertPath = (String) parameter.get("dockerCertPath");
		//
		DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
				.withDockerTlsVerify(StrUtil.isNotEmpty(dockerCertPath))
				.withApiVersion(apiVersion)
				.withDockerCertPath(dockerCertPath)
				.withDockerHost(host).build();
		//
		ApacheDockerHttpClient.Builder builder = new ApacheDockerHttpClient.Builder()
				.dockerHost(config.getDockerHost())
				.sslConfig(config.getSSLConfig())
				.maxConnections(maxConnections);
		//
		int timeout = Convert.toInt(parameter.get("timeout"), 0);
		if (timeout > 0) {
			builder.connectionTimeout(Duration.ofSeconds(timeout));
			builder.responseTimeout(Duration.ofSeconds(timeout));
		}
		ApacheDockerHttpClient httpClient = builder.build();
		return DockerClientImpl.getInstance(config, httpClient);
	}
}
