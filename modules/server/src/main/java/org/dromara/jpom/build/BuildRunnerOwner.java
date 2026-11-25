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
 * 构建执行器所属的「编排者」契约
 * <p>
 * 执行器（本机/远程节点）只依赖这两件事：拿到要执行的脚本、产物在通配模式下改写结果目录。
 * 抽成接口后，在线构建（{@link BuildExecuteManage}）与流水线可以共用同一套节点执行器，
 * 不必为流水线再写一份节点协议实现。
 *
 * @author jpom
 * @since 2.11.16
 */
public interface BuildRunnerOwner {

    /**
     * 取本次要执行的脚本内容
     *
     * @return 脚本；null 表示没有配置
     */
    String preparedScript();

    /**
     * 产物为 ant 通配模式时，把结果目录改写为根目录
     */
    void updateResultDirToRoot();
}
