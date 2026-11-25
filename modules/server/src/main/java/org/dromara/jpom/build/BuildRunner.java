/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.build;

/**
 * 构建「执行阶段」契约。
 * <p>
 * 构建的编排（阶段顺序、webhook、状态更新、产物发布）都在 {@link BuildExecuteManage}；
 * 真正干活的三步（拉代码、执行命令、收集产物）由本接口的实现承担，用来把「执行地点」
 * 从 Server 本机换成 Go 插件端节点。
 * <p>
 * 新增一种执行地点 = 新增一个实现类 + 在 {@link BuildRunnerFactory} 注册一行，
 * 调用方（{@link BuildExecuteManage#createProcess()}）零改动。
 *
 * @author jpom
 * @since 2.11.15
 */
public interface BuildRunner {

    /**
     * 准备阶段：清理构建缓存、准备构建环境变量
     *
     * @return 错误信息，null 表示成功
     */
    String startReady();

    /**
     * 拉取仓库代码
     *
     * @return 错误信息，null 表示成功
     */
    String pull();

    /**
     * 执行构建命令
     *
     * @return 错误信息，null 表示成功
     */
    String executeCommand();

    /**
     * 收集构建产物到本地结果目录
     *
     * @return 错误信息，null 表示成功
     */
    String packageFile();

    /**
     * 取消当前构建（幂等）
     * <p>
     * 返回值语义是「确认已停止」而不是「发过取消」：本机执行器按进程树是否全部退出判断，
     * 节点执行器要等节点回报结束（回执）才返回 true。上层据此决定落「已取消」还是「异常关闭」。
     * </p>
     *
     * @param reason 取消原因
     * @return true 确认已停止
     */
    boolean cancel(String reason);
}
