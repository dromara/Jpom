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
package io.jpom.controller.node;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.Page;
import cn.hutool.db.sql.Direction;
import cn.hutool.db.sql.Order;
import io.jpom.common.BaseServerController;
import io.jpom.common.JsonMessage;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.func.assets.model.MachineNodeStatLogModel;
import io.jpom.func.assets.server.MachineNodeStatLogServer;
import io.jpom.model.data.NodeModel;
import io.jpom.permission.SystemPermission;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 节点统计信息
 *
 * @author bwcx_jzy
 */
@RestController
@RequestMapping(value = "/node")
public class NodeWelcomeController extends BaseServerController {

    private final MachineNodeStatLogServer machineNodeStatLogServer;

    public NodeWelcomeController(MachineNodeStatLogServer machineNodeStatLogServer) {
        this.machineNodeStatLogServer = machineNodeStatLogServer;
    }

    @PostMapping(value = "node_monitor_data.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonMessage<List<MachineNodeStatLogModel>> nodeMonitorJson() {
        List<MachineNodeStatLogModel> list = this.getList();
        Assert.notEmpty(list, "没有查询到任何数据");
        return JsonMessage.success("ok", list);
    }

    private List<MachineNodeStatLogModel> getList() {
        NodeModel node = getNode();
        String machineId = node.getMachineId();
        String startDateStr = getParameter("time[0]");
        String endDateStr = getParameter("time[1]");
        if (StrUtil.hasEmpty(startDateStr, endDateStr)) {
            MachineNodeStatLogModel systemMonitorLog = new MachineNodeStatLogModel();
            systemMonitorLog.setMachineId(machineId);
            return machineNodeStatLogServer.queryList(systemMonitorLog, 500, new Order("monitorTime", Direction.DESC));
        }
        //  处理时间
        DateTime startDate = DateUtil.parse(startDateStr);
        long startTime = startDate.getTime();
        DateTime endDate = DateUtil.parse(endDateStr);
        if (startDate.equals(endDate)) {
            // 时间相等
            endDate = DateUtil.endOfDay(endDate);
        }
        long endTime = endDate.getTime();
        // 开启了节点信息采集
        Page pageObj = new Page(1, 5000);
        pageObj.addOrder(new Order("monitorTime", Direction.DESC));
        Entity entity = Entity.create();
        entity.set("machineId", machineId);
        entity.set(" MONITORTIME", ">= " + startTime);
        entity.set("MONITORTIME", "<= " + endTime);
        return machineNodeStatLogServer.listPageOnlyResult(entity, pageObj);
    }

    @RequestMapping(value = "processList", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String getProcessList() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.ProcessList).toString();
    }

    @RequestMapping(value = "kill.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @SystemPermission
    public String kill() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.Kill).toString();
    }
}
