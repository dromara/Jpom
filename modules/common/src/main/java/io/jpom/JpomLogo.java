/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
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
