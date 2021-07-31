package io.jpom.system.init;

import cn.jiangzeyin.common.DefaultSystemLog;
import io.jpom.system.ServerExtConfigBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.boot.autoconfigure.h2.H2ConsoleProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Hotstrip
 * init H2ConsoleProperties use extConfig.yml file
 */
@Configuration
public class InitH2Console implements InstantiationAwareBeanPostProcessor {

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof H2ConsoleProperties) {
			DefaultSystemLog.getLog().info("InitH2Console...set h2.console.enabled: [{}]",
					ServerExtConfigBean.getInstance().isH2ConsoleEnabled());
			H2ConsoleProperties h2ConsoleProperties = (H2ConsoleProperties) bean;
			h2ConsoleProperties.setEnabled(ServerExtConfigBean.getInstance().isH2ConsoleEnabled());
		}
		return bean;
	}

	/**
	 * @author Hotstrip
	 * return an H2ConsoleProperties instance, it will be set properties by
	 * @see InitH2Console#postProcessAfterInitialization
	 * @return
	 */
	@Bean
	public H2ConsoleProperties initH2ConsoleProperties() {
		return new H2ConsoleProperties();
	}
}
