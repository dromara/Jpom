/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package i8n;

import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.junit.Test;

import java.util.Locale;

/**
 * @author bwcx_jzy
 * @since 2024/7/12
 */
public class TestHttpHeader {

    @Test
    public void test() {
        this.parse("zh-CN,zh;q=0.8,en;q=0.2");
    }

    @Test
    public void test2() {
        String string = I18nMessageUtil.headerAcceptLanguageBest("zh-CN,zh;q=0.8,en;q=0.2");
        System.out.println(string);

        string = I18nMessageUtil.headerAcceptLanguageBest("zh-CN;q=0.7,zh;q=0.8,en;q=0.2");
        System.out.println(string);
    }

    private void parse(String acceptLanguageHeader) {
        if (acceptLanguageHeader != null && !acceptLanguageHeader.isEmpty()) {
            // Split the header into individual language tags
            String[] languageTags = acceptLanguageHeader.split(",");

            for (String tag : languageTags) {
                // Trim the tag and split it into language and possibly country
                String[] parts = tag.trim().split(";");

                Locale locale = null;
                float quality = 1.0f; // Default quality is 1.0

                if (parts.length > 0) {
                    // The first part is the language code
                    String lang = parts[0];
                    if (lang.contains("-")) {
                        // If there's a dash, we have a language and country
                        String[] langParts = lang.split("-");
                        locale = new Locale(langParts[0], langParts[1].toUpperCase());
                    } else {
                        // Otherwise, just the language
                        locale = new Locale(lang);
                    }

                    if (parts.length > 1) {
                        // If there's a second part, it's the quality factor
                        String qPart = parts[1].trim();
                        if (qPart.startsWith("q=")) {
                            try {
                                quality = Float.parseFloat(qPart.substring(2));
                            } catch (NumberFormatException e) {
                                // Ignore if parsing fails
                            }
                        }
                    }
                }

                System.out.println("Language: " + locale + ", Quality: " + quality);
            }
        } else {
            System.out.println("No Accept-Language header found.");
        }
    }
}
