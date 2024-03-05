/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import org.dromara.jpom.util.CommandUtil;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.function.BiConsumer;

public class TestCommandWget {


    @Test
    public void test() throws IOException, InterruptedException {
        File userHomeDir = FileUtil.getUserHomeDir();
        String url = "https://mirrors.aliyun.com/apache/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz";
        File file = FileUtil.file(userHomeDir, ".jpom", "test-curl." + CommandUtil.SUFFIX);
        // curl -LfSo maven.tar.gz https://mirrors.aliyun.com/apache/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz
        FileUtil.writeUtf8String(StrUtil.format("curl -LfSo {}/maven.tar.gz {}", userHomeDir.getAbsolutePath(), url), file);
//
        long time = System.currentTimeMillis();
        int waitFor = CommandUtil.execWaitFor(file, null, null, StrUtil.EMPTY, new BiConsumer<String, Process>() {
            @Override
            public void accept(String s, Process process) {
                System.out.println(s);
            }
        });
        System.out.println(waitFor + "  " + DateUtil.formatBetween(System.currentTimeMillis() - time));
    }

}
