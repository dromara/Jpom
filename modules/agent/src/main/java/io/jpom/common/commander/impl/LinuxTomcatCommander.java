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

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.StrSplitter;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import io.jpom.common.commander.AbstractTomcatCommander;
import io.jpom.model.data.TomcatInfoModel;
import io.jpom.util.CommandUtil;

import java.util.List;

/**
 * tomcat的linux管理命令
 *
 * @author LF
 */
public class LinuxTomcatCommander extends AbstractTomcatCommander {

    @Override
    public String execCmd(TomcatInfoModel tomcatInfoModel, String cmd) {
        String tomcatPath = tomcatInfoModel.pathAndCheck();
        if (StrUtil.isBlank(tomcatPath)) {
            return "tomcat path blank";
        }
        String command = null;
        if ("stop".equals(cmd)) {
            String setPidCmd = CommandUtil.execSystemCommand("jps -mv");
            List<String> list = StrSplitter.splitTrim(setPidCmd, StrUtil.LF, true);
            for (String item : list) {
                //路径格式转换
                String msg = FileUtil.normalize(item + StrUtil.SLASH);
                //判断集合中元素是否包含指定Tomcat路径
                boolean w = msg.contains(tomcatInfoModel.getPath());
                if (w) {
                    //截取TomcatPid
                    if (msg.indexOf(" ") > 1) {
                        String tmPid = msg.substring(0, msg.indexOf(" "));
                        //判断截取的PID是否为纯数字
                        if (NumberUtil.isInteger(tmPid)) {
                            command = String.format("kill -9 %s", tmPid);
                            exec(command, false);
                        }
                    }
                }
            }
        } else {
            command = String.format("/bin/sh -c %s/bin/startup.sh", tomcatPath);
            //
            exec(command, false);
        }
        if (StrUtil.isBlank(tomcatPath)) {
            return "tomcat path blank";
        }
        // 查询操作结果并返回
        return getStatus(tomcatInfoModel, cmd);
    }
}
