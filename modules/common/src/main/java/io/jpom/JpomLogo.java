package io.jpom;

import cn.jiangzeyin.common.DefaultSystemLog;
import io.jpom.util.VersionUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.logging.LoggingApplicationListener;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;

/**
 * @author Hotstrip
 * load Jpom version and print Jpom logo
 */
//@Order(LoggingApplicationListener.DEFAULT_ORDER + 1)
public class JpomLogo implements ApplicationListener<ApplicationReadyEvent> {

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        System.out.println(buildBannerText());
    }

    private static final String JPOM_LOGO = "\n" +
            "       _                       \n" +
            "      | |                      \n" +
            "      | |_ __   ___  _ __ ___  \n" +
            "  _   | | '_ \\ / _ \\| '_ ` _ \\ \n" +
            " | |__| | |_) | (_) | | | | | |\n" +
            "  \\____/| .__/ \\___/|_| |_| |_|\n" +
            "        | |                    \n" +
            "        |_|                    \n";

    private String buildBannerText() {
        String LINE_SEPARATOR = System.getProperty("line.separator");
        return LINE_SEPARATOR
                + JPOM_LOGO
                + LINE_SEPARATOR
                + " :: Jpom :: (v"+ VersionUtils.getVersion(getClass(), "2.4.8") +")"
                + LINE_SEPARATOR;
    }

}
