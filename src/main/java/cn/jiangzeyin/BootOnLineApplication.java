package cn.jiangzeyin;

import cn.jiangzeyin.common.BaseApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.ComponentScan;

/**
 * Created by jiangzeyin on 2017/9/14.
 */
@SpringBootApplication
@ServletComponentScan
@ComponentScan({"cn.jiangzeyin"})
public class BootOnLineApplication extends BaseApplication {

    /**
     * 启动执行
     *
     * @param args 参数
     */
    public static void main(String[] args) {
        BaseApplication application = new BaseApplication(BootOnLineApplication.class);
        application.run(args);
    }
}
