/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.dromara.jpom.func.assets.server;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.db.Entity;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.ISystemTask;
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

    private final ServerConfig.NodeConfig nodeConfig;

    public MachineNodeStatLogServer(ServerConfig serverConfig) {
        this.nodeConfig = serverConfig.getNode();
    }

    @Override
    public void executeTask() {
        int statLogKeepDays = nodeConfig.getStatLogKeepDays();
        log.debug("统计日志保留天数 {}", statLogKeepDays);
        if (statLogKeepDays <= 0) {
            return;
        }
        Entity entity = Entity.create();
        DateTime dateTime = DateUtil.beginOfDay(DateTime.now());
        dateTime = DateUtil.offsetDay(dateTime, -statLogKeepDays);
        entity.set(" monitorTime", "< " + dateTime.getTime());
        int del = this.del(entity);
        log.info("自动清理 {} 条机器节点统计日志", del);
    }
}
