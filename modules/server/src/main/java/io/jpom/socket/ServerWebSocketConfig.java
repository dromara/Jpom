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
package io.jpom.socket;

import io.jpom.socket.handler.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * socket 配置
 *
 * @author jiangzeyin
 */
@Configuration
@EnableWebSocket
public class ServerWebSocketConfig implements WebSocketConfigurer {
	private final ServerWebSocketInterceptor serverWebSocketInterceptor = new ServerWebSocketInterceptor();
//    private final ServerNodeUpdateWebSocketInterceptor serverNodeUpdateWebSocketInterceptor = new ServerNodeUpdateWebSocketInterceptor();

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		// 控制台
		registry.addHandler(new ConsoleHandler(), "/socket/console")
				.addInterceptors(serverWebSocketInterceptor).setAllowedOrigins("*");
		// 节点脚本模板
		registry.addHandler(new NodeScriptHandler(), "/socket/node/script_run")
				.addInterceptors(serverWebSocketInterceptor).setAllowedOrigins("*");
		// tomcat
		registry.addHandler(new TomcatHandler(), "/socket/tomcat_log")
				.addInterceptors(serverWebSocketInterceptor).setAllowedOrigins("*");
		// ssh
		registry.addHandler(new SshHandler(), "/socket/ssh")
				.addInterceptors(serverWebSocketInterceptor).setAllowedOrigins("*");
		// 节点升级
		registry.addHandler(new NodeUpdateHandler(), "/socket/node_update")
				.addInterceptors(serverWebSocketInterceptor).setAllowedOrigins("*");
		// 脚本模板
		registry.addHandler(new ServerScriptHandler(), "/socket/script_run")
				.addInterceptors(serverWebSocketInterceptor).setAllowedOrigins("*");
		// docker log
		registry.addHandler(new DockerLogHandler(), "/socket/docker_log")
				.addInterceptors(serverWebSocketInterceptor).setAllowedOrigins("*");
		// docker cli
		registry.addHandler(new DockerCliHandler(), "/socket/docker_cli")
				.addInterceptors(serverWebSocketInterceptor).setAllowedOrigins("*");
	}
}
