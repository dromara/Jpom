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
package io.jpom.common.commander.impl;

import cn.hutool.core.util.StrUtil;
import io.jpom.common.commander.AbstractSystemCommander;
import io.jpom.util.CommandUtil;

import java.io.File;

/**
 * windows 系统查询命令
 *
 * @author jiangzeyin
 * @since 2019/4/16
 */
public class WindowsSystemCommander extends AbstractSystemCommander {

    @Override
    public String emptyLogFile(File file) {
        return CommandUtil.execSystemCommand("echo  \"\" > " + file.getAbsolutePath());
    }


    @Override
    public boolean getServiceStatus(String serviceName) {
        String result = CommandUtil.execSystemCommand("sc query " + serviceName);
        return StrUtil.containsIgnoreCase(result, "RUNNING");
    }

    @Override
    public String startService(String serviceName) {
        String format = StrUtil.format("net start {}", serviceName);
        return CommandUtil.execSystemCommand(format);
    }

    @Override
    public String stopService(String serviceName) {
        String format = StrUtil.format("net stop {}", serviceName);
        return CommandUtil.execSystemCommand(format);
    }

    @Override
    public String buildKill(int pid) {
        return String.format("taskkill /F /PID %s", pid);
    }
}
