package cn.keepbx.jpom.system;

import cn.jiangzeyin.common.spring.SpringUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 外部配置文件
 *
 * @author jiangzeyin
 * @date 2019/3/04
 */
@Configuration
public class ExtConfigBean {

    public static final String FILE_NAME ="extConfig.yml";

    /**
     * 白名单路径是否判断包含关系
     */
    @Value("${whitelistDirectory.checkStartsWith:true}")
    public boolean whitelistDirectoryCheckStartsWith = true;


    public static ExtConfigBean getInstance() {
        return SpringUtil.getBean(ExtConfigBean.class);
////        String jPomPath = ConfigBean.getInstance().getPath();
////        File file = new File(jPomPath + "/extConfig.yml");
////        if (!file.exists()) {
////            return Singleton.get(ExtConfigBean.class);
////        }
////        YamlPropertySourceLoader yamlPropertySourceLoader = new YamlPropertySourceLoader();
//        ClassPathResource classPathResource = new ClassPathResource("/extConfig.yml");
////        PropertySource propertySource = yamlPropertySourceLoader.load("test", classPathResource, null);
//        YamlPropertiesFactoryBean yamlPropertiesFactoryBean = new YamlPropertiesFactoryBean();
//        yamlPropertiesFactoryBean.setResources(classPathResource);
//        Properties properties = yamlPropertiesFactoryBean.getObject();
//        System.out.println(properties);
////        PathResource fileResource = new PathResource(file.toURI());
////        YAML_CONFIGURATION_FACTORY.setResource(fileResource);
////        try {
////            return YAML_CONFIGURATION_FACTORY.getObject();
////        } catch (Exception e) {
////            DefaultSystemLog.ERROR().error("加载外部配置错误", e);
////        }
//
//
//        ConfigurableApplicationContext configurableApplicationContext = (ConfigurableApplicationContext) SpringUtil.getApplicationContext();
//
//        // 获取bean工厂并转换为DefaultListableBeanFactory
//        DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory) configurableApplicationContext.getBeanFactory();
//
//        // 通过BeanDefinitionBuilder创建bean定义
//        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(ExtConfigBean.class);
//
//        // 设置属性userService,此属性引用已经定义的bean:userService,这里userService已经被spring容器管理了.
////        beanDefinitionBuilder.addPropertyReference("testService", "testService");
//
//        // 注册bean
//        defaultListableBeanFactory.registerBeanDefinition("testController", beanDefinitionBuilder.getRawBeanDefinition());
//
//
//        return Singleton.get(ExtConfigBean.class);
    }


}
