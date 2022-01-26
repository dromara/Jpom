package io.jpom;

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
	 * @param timeout        超时时间
	 * @return DockerClient
	 */
	public static DockerClient build(Map<String, Object> parameter, int maxConnections, int timeout) {
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
		if (timeout > 0) {
			builder.connectionTimeout(Duration.ofSeconds(timeout));
			builder.responseTimeout(Duration.ofSeconds(timeout));
		}
		ApacheDockerHttpClient httpClient = builder.build();
		return DockerClientImpl.getInstance(config, httpClient);
	}
}
