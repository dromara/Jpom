/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.socket;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;
import org.springframework.web.context.support.WebApplicationObjectSupport;

import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 插件端socket 配置
 *
 * @author bwcx_jzy
 * @since 2019/4/19
 */
@Configuration
public class AgentWebSocketConfig extends WebApplicationObjectSupport implements SmartInitializingSingleton {


    private ServerContainer getServerContainer() {
        return (ServerContainer) Objects.requireNonNull(getServletContext()).getAttribute("javax.websocket.server.ServerContainer");
    }

    @Override
    protected boolean isContextRequired() {
        return true;
    }

    @Override
    public void afterSingletonsInstantiated() {
        registerEndpoints();
    }

    protected void registerEndpoints() {
        Set<Class<?>> endpointClasses = new LinkedHashSet<>();

        ApplicationContext context = getApplicationContext();
        if (context != null) {
            String[] endpointBeanNames = context.getBeanNamesForAnnotation(ServerEndpoint.class);
            for (String beanName : endpointBeanNames) {
                endpointClasses.add(context.getType(beanName));
            }
        }

        for (Class<?> endpointClass : endpointClasses) {
            registerEndpoint(endpointClass);
        }

        if (context != null) {
            Map<String, ServerEndpointConfig> endpointConfigMap = context.getBeansOfType(ServerEndpointConfig.class);
            for (ServerEndpointConfig endpointConfig : endpointConfigMap.values()) {
                registerEndpoint(endpointConfig);
            }
        }
    }

    private void registerEndpoint(Class<?> endpointClass) {
        ServerContainer serverContainer = getServerContainer();
        Assert.state(serverContainer != null,
                "No ServerContainer set. Most likely the server's own WebSocket ServletContainerInitializer " +
                        "has not run yet. Was the Spring ApplicationContext refreshed through a " +
                        "org.springframework.web.context.ContextLoaderListener, " +
                        "i.e. after the ServletContext has been fully initialized?");
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Registering @ServerEndpoint class: " + endpointClass);
            }
            serverContainer.addEndpoint(endpointClass);
        } catch (DeploymentException ex) {
            throw new IllegalStateException("Failed to register @ServerEndpoint class: " + endpointClass, ex);
        }
    }

    private void registerEndpoint(ServerEndpointConfig endpointConfig) {
        ServerContainer serverContainer = this.getServerContainer();
        Assert.state(serverContainer != null, "No ServerContainer set");
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Registering ServerEndpointConfig: " + endpointConfig);
            }
            serverContainer.addEndpoint(endpointConfig);
        } catch (DeploymentException ex) {
            throw new IllegalStateException("Failed to register ServerEndpointConfig: " + endpointConfig, ex);
        }
    }
}
