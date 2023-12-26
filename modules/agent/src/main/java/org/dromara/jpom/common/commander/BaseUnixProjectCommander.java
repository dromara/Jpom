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
package org.dromara.jpom.common.commander;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import org.dromara.jpom.model.data.NodeProjectInfoModel;
import org.dromara.jpom.util.CommandUtil;
import org.dromara.jpom.util.JvmUtil;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * unix
 *
 * @author bwcx_jzy
 * @since 2021/12/17
 */
public abstract class BaseUnixProjectCommander extends AbstractProjectCommander {


    public BaseUnixProjectCommander(Charset fileCharset) {
        super(fileCharset);
    }

    @Override
    public String buildJavaCommand(NodeProjectInfoModel nodeProjectInfoModel) {
        String path = NodeProjectInfoModel.getClassPathLib(nodeProjectInfoModel);
        if (StrUtil.isBlank(path)) {
            return null;
        }
        String tag = nodeProjectInfoModel.getId();
        return StrUtil.format("nohup {} {} {} {} {} {} >> {} 2>&1 &",
            getRunJavaPath(nodeProjectInfoModel, false),
            Optional.ofNullable(nodeProjectInfoModel.getJvm()).orElse(StrUtil.EMPTY),
            JvmUtil.getJpomPidTag(tag, nodeProjectInfoModel.allLib()),
            path,
            Optional.ofNullable(nodeProjectInfoModel.getMainClass()).orElse(StrUtil.EMPTY),
            Optional.ofNullable(nodeProjectInfoModel.getArgs()).orElse(StrUtil.EMPTY),
            nodeProjectInfoModel.getAbsoluteLog());
    }

    @Override
    public CommandOpResult stopJava(NodeProjectInfoModel nodeProjectInfoModel, int pid) throws Exception {
        File file = FileUtil.file(nodeProjectInfoModel.allLib());
        List<String> result = new ArrayList<>();
        boolean success = false;
        String kill = AbstractSystemCommander.getInstance().kill(file, pid);
        result.add(kill);
        if (this.loopCheckRun(nodeProjectInfoModel, false)) {
            success = true;
        } else {
            // 强制杀进程
            result.add("Kill not completed, test kill -9");
            String cmd = String.format("kill -9 %s", pid);
            CommandUtil.asyncExeLocalCommand(file, cmd);
            //
            if (this.loopCheckRun(nodeProjectInfoModel, 5, false)) {
                success = true;
            } else {
                result.add("Kill -9 not completed, kill -9 failed ");
            }
        }
        String tag = nodeProjectInfoModel.getId();
        return CommandOpResult.of(success, status(tag)).appendMsg(result);
//        return status(tag) + StrUtil.SPACE + kill;
    }
}
