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

import org.dromara.jpom.model.data.NodeProjectInfoModel;
import org.dromara.jpom.socket.ConsoleCommandOp;

/**
 * @author bwcx_jzy
 * @since 23/12/29 029
 */
public interface ProjectCommander {

    /**
     * 生成可以执行的命令
     *
     * @param nodeProjectInfoModel 项目
     * @return null 是条件不足
     */
    String buildRunCommand(NodeProjectInfoModel nodeProjectInfoModel);

    /**
     * 执行 webhooks 通知
     *
     * @param nodeProjectInfoModel 项目信息
     * @param type                 类型
     * @param other                其他参数
     */
    void asyncWebHooks(NodeProjectInfoModel nodeProjectInfoModel, String type, Object... other);


    /**
     * 清空日志信息
     *
     * @param nodeProjectInfoModel 项目
     * @return 结果
     */
    String backLog(NodeProjectInfoModel nodeProjectInfoModel);


    /**
     * 获取进程占用的主要端口
     *
     * @param pid 进程id
     * @return 端口
     */
    String getMainPort(int pid);


    /**
     * 是否正在运行
     *
     * @param nodeProjectInfoModel 项目
     * @return true 正在运行
     */
    boolean isRun(NodeProjectInfoModel nodeProjectInfoModel);


    /**
     * 执行shell命令
     *
     * @param consoleCommandOp     执行的操作
     * @param nodeProjectInfoModel 项目信息
     * @return 执行结果
     */
    CommandOpResult execCommand(ConsoleCommandOp consoleCommandOp, NodeProjectInfoModel nodeProjectInfoModel);
}
