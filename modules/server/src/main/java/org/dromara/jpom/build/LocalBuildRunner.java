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
 * Server 本机执行器：把编排层原有的四个阶段转回 {@link BuildExecuteManage}，
 * 保证老构建（未配置构建节点）行为逐字不变。
 *
 * @author jpom
 * @since 2.11.15
 */
class LocalBuildRunner implements BuildRunner {

    private final BuildExecuteManage owner;

    LocalBuildRunner(BuildExecuteManage owner) {
        this.owner = owner;
    }

    @Override
    public String startReady() {
        return owner.localStartReady();
    }

    @Override
    public String pull() {
        return owner.localPull();
    }

    @Override
    public String executeCommand() {
        return owner.localExecuteCommand();
    }

    @Override
    public String packageFile() {
        return owner.localPackageFile();
    }

    @Override
    public void cancel(String reason) {
        owner.localCancel(reason);
    }
}
