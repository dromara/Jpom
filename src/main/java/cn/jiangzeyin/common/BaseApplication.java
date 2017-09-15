package cn.jiangzeyin.common;
/**
 * Created by jiangzeyin on 2017/1/10.
 */

import org.springframework.boot.SpringApplication;

/**
 * @author jiangzeyin
 * @create 2017 01 10 16:22
 */
public class BaseApplication extends SpringApplication {

    /**
     * @param sources
     */
    public BaseApplication(Object... sources) {
        super(sources);
        setBanner((environment, sourceClass, out) -> {
            String describe = environment.getProperty("describe");

            out.println("优客创想 " + describe + " 系统启动中");
        });
    }
}
