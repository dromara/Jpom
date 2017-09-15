package cn.jiangzeyin.common.interceptor;

import cn.jiangzeyin.system.log.SystemLog;
import cn.jiangzeyin.util.PackageUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.List;

/**
 * 拦截器控制器
 *
 * @author jiangzeyin
 * Created by jiangzeyin on 2017/2/4.
 */
@Configuration
@EnableWebMvc
public class InterceptorControl extends WebMvcConfigurerAdapter {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //registry.addInterceptor(new LoginInterceptor()).addPathPatterns("/**");
        init(registry);
    }

    /**
     * @param registry
     */
    private void init(InterceptorRegistry registry) {
        List<String> list = null;
        try {
            list = PackageUtil.getClassName("com.yoke.common.interceptor");
        } catch (IOException e) {
        }
        if (list == null)
            return;
        for (String item : list) {
            Class classItem = null;
            try {
                classItem = Class.forName(item);
            } catch (ClassNotFoundException e) {
                SystemLog.ERROR().error("加载拦截器错误", e);
            }
            if (classItem == null)
                continue;
            boolean isAbstract = Modifier.isAbstract(classItem.getModifiers());
            if (isAbstract)
                continue;
            InterceptorUrl interceptorUrl = (InterceptorUrl) classItem.getAnnotation(InterceptorUrl.class);
            if (interceptorUrl == null)
                continue;
            HandlerInterceptor handlerInterceptor = null;
            try {
                handlerInterceptor = (HandlerInterceptor) classItem.newInstance();
            } catch (InstantiationException e) {
                SystemLog.ERROR().error("加载拦截器错误", e);
            } catch (IllegalAccessException e) {
                SystemLog.ERROR().error("加载拦截器错误", e);
            }
            if (handlerInterceptor == null)
                continue;
            String[] patterns = interceptorUrl.value();
            if (patterns == null)
                continue;
            registry.addInterceptor(handlerInterceptor).addPathPatterns(patterns);
            SystemLog.LOG().info("加载拦截器：" + classItem + "  " + patterns[0]);
        }
    }
}
