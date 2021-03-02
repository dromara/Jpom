package io.jpom;

import cn.hutool.core.io.FileUtil;
import io.jpom.common.JpomManifest;
import io.jpom.util.VersionUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

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

    /**
     * @return jpom logo banner
     * @see JpomManifest#getVersion()
     */
    private String buildBannerText() {
        String lineSeparator = FileUtil.getLineSeparator();
        return lineSeparator
                + JPOM_LOGO
                + lineSeparator
                + " :: Jpom :: (v" + VersionUtils.getVersion(getClass(), JpomManifest.getInstance().getVersion()) + ")"
                + lineSeparator;
    }

}
