/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
import cn.hutool.core.lang.PatternPool;
import cn.hutool.core.util.ReUtil;
import org.junit.Test;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author bwcx_jzy
 * @since 2024/6/16
 */
public class TestScriptLink {

    @Test
    public void test() {
        String content = "test\n" +
            "\n" +
            "echo \"test -12\"\n" +
            "\n" +
            "echo $buildNumberId\n" +
            "G@(\"xxxx\")\n" +
            "\n" +
            "G@(\"xxxx\")\n" +
            "\n" +
            "G@(\"ttttt\")";

        String reg = "G@\\(\"(.*?)\"\\)";
        Pattern pattern = PatternPool.get("G@\\(\"(.*?)\"\\)", Pattern.DOTALL);
        List<String> all = ReUtil.findAll(reg, content, 1);
        all.forEach(System.out::println);

        Matcher matcher = pattern.matcher(content);
        StringBuffer modifiedLine = new StringBuffer();
        while (matcher.find()) {
            String group = matcher.group(1);
            matcher.appendReplacement(modifiedLine, String.format("\"%s\"", group));
        }
        matcher.appendTail(modifiedLine);
        System.out.println(modifiedLine);
    }
}
