/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.func.assets.server;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.db.Entity;
import cn.keepbx.jpom.event.ISystemTask;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.configuration.NodeConfig;
import org.dromara.jpom.func.assets.model.MachineNodeStatLogModel;
import org.dromara.jpom.service.h2db.BaseDbService;
import org.dromara.jpom.system.ServerConfig;
import org.springframework.stereotype.Service;

/**
 * @author bwcx_jzy
 * @since 2023/2/18
 */
@Service
@Slf4j
public class MachineNodeStatLogServer extends BaseDbService<MachineNodeStatLogModel> implements ISystemTask {

    private final NodeConfig nodeConfig;

    public MachineNodeStatLogServer(ServerConfig serverConfig) {
        this.nodeConfig = serverConfig.getNode();
    }

    @Override
    public void executeTask() {
        int statLogKeepDays = nodeConfig.getStatLogKeepDays();
        log.debug(I18nMessageUtil.get("i18n.log_retention_days.99d1"), statLogKeepDays);
        if (statLogKeepDays <= 0) {
            return;
        }
        Entity entity = Entity.create();
        DateTime dateTime = DateUtil.beginOfDay(DateTime.now());
        dateTime = DateUtil.offsetDay(dateTime, -statLogKeepDays);
        entity.set(" monitorTime", "< " + dateTime.getTime());
        int del = this.del(entity);
        log.info(I18nMessageUtil.get("i18n.auto_clear_machine_node_stats_logs.5279"), del);
    }
}
