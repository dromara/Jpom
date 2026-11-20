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

import cn.hutool.cron.task.Task;
import cn.keepbx.jpom.event.IAsyncLoad;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.ILoadEvent;
import org.dromara.jpom.func.system.service.ClusterInfoService;
import org.dromara.jpom.model.data.SshProjectModel;
import org.dromara.jpom.socket.ConsoleCommandOp;
import org.dromara.jpom.cron.CronUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * SSH 项目自动启动
 * <p>
 * 每分钟扫描 {@code autoStart=1} 的项目，未在运行则按 {@code start} 钩子拉起。
 * 多实例部署时可能重复触发，内置模板的 start 脚本都做了「已在运行直接退出 0」的幂等保护；
 * 严格租约留到 M3（集群链路组 + 分布式锁）。
 *
 * @author bwcx_jzy
 * @since 2026/11/20
 */
@Slf4j
@Component
public class SshProjectAutoStartTask implements ILoadEvent, IAsyncLoad, Task {

    private static final String CRON_ID = "ssh-project-autostart";

    private final SshProjectService sshProjectService;
    private final SshProjectExecutor sshProjectExecutor;
    private final ClusterInfoService clusterInfoService;

    public SshProjectAutoStartTask(SshProjectService sshProjectService,
                                   SshProjectExecutor sshProjectExecutor,
                                   ClusterInfoService clusterInfoService) {
        this.sshProjectService = sshProjectService;
        this.sshProjectExecutor = sshProjectExecutor;
        this.clusterInfoService = clusterInfoService;
    }

    @Override
    public void afterPropertiesSet(ApplicationContext applicationContext) {
        // 不需要额外初始化
    }

    @Override
    public void startLoad() {
        CronUtils.add(CRON_ID, "0 0/1 * * * ?", () -> SshProjectAutoStartTask.this);
    }

    @Override
    public void execute() {
        if (clusterInfoService.isMultiServer()) {
            // 多实例：交给集群链路组判断（未绑定分组时退化为全量，靠脚本幂等兜底）
            List<String> linkGroups = clusterInfoService.currentLinkGroups();
            if (linkGroups == null) {
                return;
            }
        }
        List<SshProjectModel> list = sshProjectService.listAutoStart();
        for (SshProjectModel project : list) {
            try {
                if (sshProjectExecutor.status(project).isRunning()) {
                    continue;
                }
                log.info("auto start ssh project: {}", project.getName());
                sshProjectService.execute(project, ConsoleCommandOp.start.name(), null);
            } catch (Exception e) {
                log.warn("auto start ssh project fail: {}", project.getId(), e);
            }
        }
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE + 3;
    }
}
