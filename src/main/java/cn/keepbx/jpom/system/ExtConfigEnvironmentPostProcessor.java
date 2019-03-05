package cn.keepbx.jpom.system;

import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigFileApplicationListener;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.CommandLinePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;

import java.io.File;
import java.io.IOException;

/**
 * 动态读取外部配置文件
 *
 * @author jiangzeyin
 * @date 2019/3/5
 */
public class ExtConfigEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        PropertySource propertySource1 = environment.getPropertySources().get(CommandLinePropertySource.COMMAND_LINE_PROPERTY_SOURCE_NAME);
        String path = null;
        if (propertySource1 != null) {
            Object object = propertySource1.getProperty(ConfigBean.JPOM_PATH);
            if (object != null) {
                path = String.valueOf(object);
            }
        }
        if (StrUtil.isEmpty(path)) {
            propertySource1 = environment.getPropertySources().get(ConfigFileApplicationListener.APPLICATION_CONFIGURATION_PROPERTY_SOURCE_NAME);
            if (propertySource1 != null) {
                Object object = propertySource1.getProperty(ConfigBean.JPOM_PATH);
                if (object != null) {
                    path = String.valueOf(object);
                }
            }
        }
        if (StrUtil.isEmpty(path)) {
            Console.error("读取配置文件错误：" + ConfigBean.JPOM_PATH);
            return;
        }
        File directory = new File(path, ExtConfigBean.FILE_NAME);
        if (!directory.exists()) {
            return;
        }
        YamlPropertySourceLoader yamlPropertySourceLoader = new YamlPropertySourceLoader();
        FileSystemResource fileSystemResource = new FileSystemResource(directory);
        try {
            PropertySource propertySource = yamlPropertySourceLoader.load(directory.getName(), fileSystemResource, null);
            environment.getPropertySources().addLast(propertySource);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
