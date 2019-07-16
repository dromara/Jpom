package cn.keepbx.jpom;

import cn.jiangzeyin.common.EnableCommonBoot;
import cn.keepbx.jpom.common.Type;
import cn.keepbx.jpom.common.interceptor.AuthorizeInterceptor;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

/**
 * jpom 启动类
 * Created by jiangzeyin on 2017/9/14.
 *
 * @author jiangzeyin
 */
@SpringBootApplication
@ServletComponentScan
@EnableCommonBoot
public class JpomAgentApplication {

    /**
     * 启动执行
     *
     * @param args 参数
     */
    public static void main(String[] args) throws Exception {
        JpomApplication jpomApplication = new JpomApplication(Type.Agent, JpomAgentApplication.class, args);
        jpomApplication
                // 拦截器
                .addInterceptor(AuthorizeInterceptor.class)
                .run(args);
    }

}
