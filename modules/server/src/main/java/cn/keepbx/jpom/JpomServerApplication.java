package cn.keepbx.jpom;

import cn.jiangzeyin.common.EnableCommonBoot;
import cn.keepbx.jpom.common.Type;
import cn.keepbx.jpom.common.interceptor.LoginInterceptor;
import cn.keepbx.jpom.common.interceptor.PermissionInterceptor;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

/**
 * jpom 启动类
 *
 * @author jiangzeyin
 * @date 2017/9/14
 */
@SpringBootApplication(scanBasePackages = {"cn.keepbx.plugin", "cn.keepbx.jpom"})
@ServletComponentScan
@EnableCommonBoot
public class JpomServerApplication {


    /**
     * 启动执行
     *
     * @param args 参数
     */
    public static void main(String[] args) throws Exception {
        JpomApplication jpomApplication = new JpomApplication(Type.Server, JpomServerApplication.class, args);
        jpomApplication
                // 拦截器
                .addInterceptor(LoginInterceptor.class)
                .addInterceptor(PermissionInterceptor.class)
                .run(args);
    }


}
