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
        File file = FileUtil.file(userHomeDir, ".jpom", "test-curl.sh");
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
