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
package io.jpom.common.commander;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import io.jpom.model.data.NodeProjectInfoModel;
import io.jpom.util.CommandUtil;
import io.jpom.util.JvmUtil;

import java.io.File;

/**
 * unix
 *
 * @author bwcx_jzy
 * @since 2021/12/17
 */
public abstract class BaseUnixProjectCommander extends AbstractProjectCommander {


    @Override
    public String buildJavaCommand(NodeProjectInfoModel nodeProjectInfoModel, NodeProjectInfoModel.JavaCopyItem javaCopyItem) {
        String path = NodeProjectInfoModel.getClassPathLib(nodeProjectInfoModel);
        if (StrUtil.isBlank(path)) {
            return null;
        }
        String tag = javaCopyItem == null ? nodeProjectInfoModel.getId() : javaCopyItem.getTagId();
        return String.format("nohup %s %s %s" +
                " %s  %s  %s >> %s 2>&1 &",
            getRunJavaPath(nodeProjectInfoModel, false),
            javaCopyItem == null ? nodeProjectInfoModel.getJvm() : javaCopyItem.getJvm(),
            JvmUtil.getJpomPidTag(tag, nodeProjectInfoModel.allLib()),
            path,
            nodeProjectInfoModel.getMainClass(),
            javaCopyItem == null ? nodeProjectInfoModel.getArgs() : javaCopyItem.getArgs(),
            nodeProjectInfoModel.getAbsoluteLog(javaCopyItem));
    }

    @Override
    public String stopJava(NodeProjectInfoModel nodeProjectInfoModel, NodeProjectInfoModel.JavaCopyItem javaCopyItem, int pid) throws Exception {
        File file = FileUtil.file(nodeProjectInfoModel.allLib());
        String kill = AbstractSystemCommander.getInstance().kill(file, pid);
        if (this.loopCheckRun(nodeProjectInfoModel, javaCopyItem, false)) {
            // 强制杀进程
            String cmd = String.format("kill -9 %s", pid);
            CommandUtil.asyncExeLocalCommand(file, cmd);
            //
            if (this.loopCheckRun(nodeProjectInfoModel, javaCopyItem, 5, false)) {
                kill += " kill failed";
            }
        }
        String tag = javaCopyItem == null ? nodeProjectInfoModel.getId() : javaCopyItem.getTagId();
        return status(tag) + StrUtil.SPACE + kill;
    }
}
