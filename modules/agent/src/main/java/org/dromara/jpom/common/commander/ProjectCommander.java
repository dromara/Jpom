/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
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
    String getMainPort(Integer pid);


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
