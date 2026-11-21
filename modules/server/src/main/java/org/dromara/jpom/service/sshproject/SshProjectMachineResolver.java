/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service.sshproject;

import cn.hutool.core.util.StrUtil;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.func.assets.model.MachineSshModel;
import org.dromara.jpom.func.assets.server.MachineSshServer;
import org.dromara.jpom.model.data.SshModel;
import org.dromara.jpom.model.data.SshProjectModel;
import org.dromara.jpom.service.node.ssh.SshService;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * SSH 项目 → SSH 资产解析
 *
 * @author bwcx_jzy
 * @since 2026/11/20
 */
@Component
public class SshProjectMachineResolver {

    private final SshService sshService;
    private final MachineSshServer machineSshServer;

    public SshProjectMachineResolver(SshService sshService, MachineSshServer machineSshServer) {
        this.sshService = sshService;
        this.machineSshServer = machineSshServer;
    }

    /**
     * 解析项目绑定的 SSH 资产（优先工作空间内的 ssh 记录，其次资产 id）
     *
     * @param project 项目
     * @return 资产
     */
    public MachineSshModel resolve(SshProjectModel project) {
        if (StrUtil.isNotEmpty(project.getSshId())) {
            SshModel sshModel = sshService.getByKey(project.getSshId());
            Assert.notNull(sshModel, I18nMessageUtil.get("i18n.ssh_does_not_exist.88d7"));
            return sshService.getMachineSshModel(sshModel);
        }
        Assert.hasText(project.getMachineSshId(), I18nMessageUtil.get("i18n.ssh_does_not_exist.88d7"));
        MachineSshModel machine = machineSshServer.getByKey(project.getMachineSshId(), false);
        Assert.notNull(machine, I18nMessageUtil.get("i18n.asset_ssh_not_exist.cd43"));
        return machine;
    }
}
