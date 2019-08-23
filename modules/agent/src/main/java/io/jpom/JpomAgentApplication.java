package io.jpom;

import cn.jiangzeyin.common.EnableCommonBoot;
import io.jpom.common.Type;
import io.jpom.common.interceptor.AuthorizeInterceptor;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

/**
 * jpom 启动类
 *
 * @author jiangzeyin
 * @date 2017/9/14.
 */
@SpringBootApplication
@ServletComponentScan
@EnableCommonBoot
public class JpomAgentApplication {

    /**
     * 启动执行
     *
     * @param args 参数
     * @throws Exception 异常
     */
    public static void main(String[] args) throws Exception {
        JpomApplication jpomApplication = new JpomApplication(Type.Agent, JpomAgentApplication.class, args);
        jpomApplication
                // 拦截器
                .addInterceptor(AuthorizeInterceptor.class)
                .run(args);
    }

}
