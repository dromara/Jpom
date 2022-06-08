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
package io.jpom.service.manage;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.util.StrUtil;
import io.jpom.common.commander.AbstractProjectCommander;
import io.jpom.model.data.NodeProjectInfoModel;
import io.jpom.socket.ConsoleCommandOp;
import org.springframework.stereotype.Service;

/**
 * 控制台
 * Created by jiangzeyin on 2018/9/28.
 *
 * @author jiangzeyin
 */
@Service
public class ConsoleService {
    private final ProjectInfoService projectInfoService;

    public ConsoleService(ProjectInfoService projectInfoService) {
        this.projectInfoService = projectInfoService;
    }

    /**
     * 执行shell命令
     *
     * @param consoleCommandOp     执行的操作
     * @param nodeProjectInfoModel 项目信息
     * @param copyItem             副本信息
     * @return 执行结果
     * @throws Exception 异常
     */
    public String execCommand(ConsoleCommandOp consoleCommandOp, NodeProjectInfoModel nodeProjectInfoModel, NodeProjectInfoModel.JavaCopyItem copyItem) throws Exception {
        String result;
        AbstractProjectCommander abstractProjectCommander = AbstractProjectCommander.getInstance();
        // 执行命令
        switch (consoleCommandOp) {
            case restart:
                Tuple restart = abstractProjectCommander.restart(nodeProjectInfoModel, copyItem);
                result = CollUtil.join(restart, StrUtil.COMMA);
                break;
            case start:
                result = abstractProjectCommander.start(nodeProjectInfoModel, copyItem);
                break;
            case stop:
                result = abstractProjectCommander.stop(nodeProjectInfoModel, copyItem);
                break;
            case status: {
                result = abstractProjectCommander.status(nodeProjectInfoModel, copyItem);
                break;
            }
            case top:
            case showlog:
            default:
                throw new IllegalArgumentException(consoleCommandOp + " error");
        }
        //  通知日志刷新
        if (consoleCommandOp == ConsoleCommandOp.start || consoleCommandOp == ConsoleCommandOp.restart) {
            // 修改 run lib 使用情况
            NodeProjectInfoModel modify = projectInfoService.getItem(nodeProjectInfoModel.getId());
            //
            if (copyItem != null) {
                NodeProjectInfoModel.JavaCopyItem copyItem1 = modify.findCopyItem(copyItem.getId());
                copyItem1.setModifyTime(DateUtil.now());
            }
            try {
                projectInfoService.updateItem(modify);
            } catch (Exception ignored) {
            }
        }
        return result;
    }
}
