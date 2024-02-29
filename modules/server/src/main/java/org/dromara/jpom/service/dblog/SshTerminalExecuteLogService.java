/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service.dblog;

import cn.hutool.core.date.SystemClock;
import cn.hutool.core.util.StrUtil;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.ServerConst;
import org.dromara.jpom.func.assets.model.MachineSshModel;
import org.dromara.jpom.model.data.SshModel;
import org.dromara.jpom.model.log.SshTerminalExecuteLog;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.service.h2db.BaseWorkspaceService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;


/**
 * ssh 终端执行日志
 *
 * @author bwcx_jzy
 * @since 2021/08/04
 */
@Service
public class SshTerminalExecuteLogService extends BaseWorkspaceService<SshTerminalExecuteLog> {

    @Override
    protected String[] clearTimeColumns() {
        return new String[]{"createTimeMillis"};
    }

    /**
     * 批量记录日志
     *
     * @param userInfo        操作的用户
     * @param sshItem         ssh 对象
     * @param machineSshModel 资产
     * @param ip              操作人的ip
     * @param userAgent       浏览器标识
     * @param commands        命令行
     * @param refuse          是否拒绝执行
     */
    public void batch(UserModel userInfo, MachineSshModel machineSshModel, SshModel sshItem, String ip, String userAgent, boolean refuse, List<String> commands) {
        if (machineSshModel == null) {
            // 资产信息不能为空
            return;
        }
        long optTime = SystemClock.now();
        try {
            BaseServerController.resetInfo(userInfo);
            List<SshTerminalExecuteLog> executeLogs = commands.stream()
                .filter(StrUtil::isNotEmpty)
                .map(s -> {
                    SshTerminalExecuteLog sshTerminalExecuteLog = new SshTerminalExecuteLog();
                    if (sshItem != null) {
                        sshTerminalExecuteLog.setSshId(sshItem.getId());
                        sshTerminalExecuteLog.setSshName(sshItem.getName());
                        sshTerminalExecuteLog.setWorkspaceId(sshItem.getWorkspaceId());
                    } else {
                        sshTerminalExecuteLog.setWorkspaceId(ServerConst.WORKSPACE_GLOBAL);
                    }
                    sshTerminalExecuteLog.setMachineSshId(machineSshModel.getId());
                    sshTerminalExecuteLog.setMachineSshName(machineSshModel.getName());
                    sshTerminalExecuteLog.setCommands(s);
                    sshTerminalExecuteLog.setRefuse(refuse);
                    sshTerminalExecuteLog.setCreateTimeMillis(optTime);
                    sshTerminalExecuteLog.setIp(ip);
                    sshTerminalExecuteLog.setUserAgent(userAgent);
                    //sshTerminalExecuteLog.setUserId(UserModel.getOptUserName(userInfo));
                    return sshTerminalExecuteLog;
                }).collect(Collectors.toList());
            super.insert(executeLogs);
        } finally {
            BaseServerController.removeAll();
        }
    }
}
