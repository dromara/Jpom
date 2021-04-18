package io.jpom;

import cn.jiangzeyin.common.EnableCommonBoot;
import cn.jiangzeyin.common.spring.event.ApplicationEventLoad;
import io.jpom.common.Type;
import io.jpom.common.interceptor.IpInterceptor;
import io.jpom.common.interceptor.LoginInterceptor;
import io.jpom.common.interceptor.OpenApiInterceptor;
import io.jpom.common.interceptor.PermissionInterceptor;
import io.jpom.permission.CacheControllerFeature;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

/**
 * jpom 启动类
 *
 * @author jiangzeyin
 * @date 2017/9/14
 */
@SpringBootApplication
@ServletComponentScan
@EnableCommonBoot
public class JpomServerApplication implements ApplicationEventLoad {


    /**
     * 启动执行
     *
     * @param args 参数
     * @throws Exception 异常
     */
    public static void main(String[] args) throws Exception {
        JpomApplication jpomApplication = new JpomApplication(Type.Server, JpomServerApplication.class, args);
        jpomApplication
                // 拦截器
                .addInterceptor(IpInterceptor.class)
                .addInterceptor(LoginInterceptor.class)
                .addInterceptor(OpenApiInterceptor.class)
                .addInterceptor(PermissionInterceptor.class)
                .run(args);
    }


    @Override
    public void applicationLoad() {
        CacheControllerFeature.init();
    }
}
