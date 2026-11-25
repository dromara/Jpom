/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.pipeline.executor;

/**
 * 发布执行出口
 * <p>
 * 生产实现复用既有的 7 个 {@code ReleaseTarget}（本插件端项目 / SSH / SSH 项目 / FTP / 分发 / Docker / 本地命令），
 * 测试里注入假实现——这样「多目标 / 跨方式 / 失败即停 / 失败继续」的语义可以脱离真实目标机验证。
 *
 * @author jpom
 * @since 2.11.16
 */
public interface PipelineReleaseInvoker {

    /**
     * 发布一个目标
     *
     * @param request 请求
     * @return null 表示成功；否则为错误信息
     * @throws Exception 发布异常
     */
    String release(ReleaseRequest request) throws Exception;
}
