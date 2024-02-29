/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
import com.github.dockerjava.core.NameParser;
import org.junit.Test;

import java.util.Formatter;

/**
 * @author bwcx_jzy
 * @since 2022/2/7
 */
public class TestFormatter {

    @Test
    public void test() {
        System.out.printf("${a}%n", "1");
        Formatter formatter = new Formatter();
        System.out.println(formatter.format("${a}", "1"));
    }

    @Test
    public void testTag() {
        NameParser.ReposTag reposTag = NameParser.parseRepositoryTag("192.168.33.106:10087/library/sso:3.0.0.RELEASE");
        System.out.println(reposTag);

        reposTag = NameParser.parseRepositoryTag("sso:3.0.0.RELEASE");
        System.out.println(reposTag);

        reposTag = NameParser.parseRepositoryTag("sso");
        System.out.println(reposTag);
    }
}
