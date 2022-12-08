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
package io.jpom.system;

import ch.qos.logback.core.PropertyDefinerBase;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.system.SystemUtil;
import org.springframework.util.Assert;

import java.io.File;

/**
 * @author bwcx_jzy
 * @since 2022/12/7
 */
public class LogbackConfig extends PropertyDefinerBase {

    public static String getPath() {
        String jpomLog = SystemUtil.get("JPOM_LOG");
        Assert.hasText(jpomLog, "没有配置 JPOM_LOG");
        return jpomLog;
    }

    @Override
    public String getPropertyValue() {
        String jpomLog = SystemUtil.get("JPOM_LOG");
        jpomLog = Opt.ofBlankAble(jpomLog).orElseGet(() -> {
            String locationPath = ClassUtil.getLocationPath(LogbackConfig.class);
            File file = FileUtil.file(FileUtil.getParent(locationPath, 2), "logs");
            String path = FileUtil.getAbsolutePath(file);
            System.out.println(path);
            return path;
        });
        SystemUtil.set("JPOM_LOG", jpomLog);
        return jpomLog;
    }
}
