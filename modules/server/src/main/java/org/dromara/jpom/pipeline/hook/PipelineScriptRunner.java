/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.pipeline.hook;

import java.util.function.Consumer;

/**
 * 脚本执行契约（同步 + 逐行回调 + 进程回调）
 * <p>
 * 流水线的「事件脚本」与「通知脚本」都走这一个出口：同步等待退出码、每行输出实时交给调用方
 * （写进运行日志）、进程句柄交给调用方登记（停止时才能杀掉）。
 * </p>
 * <p>
 * 不做成「触发一次就返回」的异步调用，原因：异步拿不到退出码也无法把输出写进运行日志，
 * 事件脚本失败就变成了静默失败（用户只看到流水线「成功」，通知其实没发出去）。
 * </p>
 *
 * @author jpom
 * @since 2.11.16
 */
public interface PipelineScriptRunner {

    /**
     * 执行服务端脚本
     *
     * @param request     执行请求
     * @param lineSink    输出行回调
     * @param processSink 进程回调（取消登记用，可为 null）
     * @return 退出码
     * @throws Exception 执行异常（脚本不存在、写文件失败等）
     */
    int run(PipelineScriptRequest request, Consumer<String> lineSink, Consumer<Process> processSink) throws Exception;
}
