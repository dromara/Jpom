package cn.jiangzeyin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.ComponentScan;

/**
 * Created by jiangzeyin on 2017/9/14.
 */
@SpringBootApplication
@ServletComponentScan
@ComponentScan({"cn.jiangzeyin"})
public class BootOnLineApplication extends SpringApplication {

    /**
     * @param sources sources
     */
    public BootOnLineApplication(Object... sources) {
        super(sources);
        setBanner((environment, sourceClass, out) -> {
            out.println("yokead Spring Boot online manager starting");
        });
    }

    /**
     * 启动执行
     *
     * @param args 参数
     */
    public static void main(String[] args) {
        BootOnLineApplication application = new BootOnLineApplication(BootOnLineApplication.class);
        application.run(args);
    }
}
