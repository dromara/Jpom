package io.jpom.controller.node;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.text.StrSpliter;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.Page;
import cn.hutool.db.PageResult;
import cn.hutool.db.sql.Direction;
import cn.hutool.db.sql.Order;
import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.JsonMessage;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.BaseEnum;
import io.jpom.model.Cycle;
import io.jpom.model.data.NodeModel;
import io.jpom.model.log.SystemMonitorLog;
import io.jpom.service.dblog.DbSystemMonitorLogService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 欢迎页
 *
 * @author Administrator
 */
@Controller
@RequestMapping(value = "/node")
public class NodeWelcomeController extends BaseServerController {

    @Resource
    private DbSystemMonitorLogService dbSystemMonitorLogService;

    private Cycle getCycle() {
        NodeModel node = getNode();
        return BaseEnum.getEnum(Cycle.class, node.getCycle());
    }

    @RequestMapping(value = "welcome", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String welcome() {
        Cycle cycle = getCycle();
        long millis = cycle == null ? TimeUnit.SECONDS.toMillis(30) : cycle.getMillis();
        if (millis <= 0) {
            millis = TimeUnit.SECONDS.toMillis(30);
        }
        if (cycle != null && cycle != Cycle.none) {
            //
            setAttribute("monitorCycle", true);
        }
        setAttribute("cycleTime", millis);
        return "node/welcome";
    }

    @RequestMapping(value = "getTop", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String getTop() {
        Cycle cycle = getCycle();
        NodeModel node = getNode();
        if (cycle == null || cycle == Cycle.none) {
            return NodeForward.request(node, getRequest(), NodeUrl.GetTop).toString();
        }
        Page pageObj = new Page(1, 60);
        pageObj.addOrder(new Order("monitorTime", Direction.DESC));
        Entity entity = Entity.create();
        entity.set("nodeId", node.getId());
        PageResult<SystemMonitorLog> pageResult = dbSystemMonitorLogService.listPage(entity, pageObj);
        List<JSONObject> series = new ArrayList<>();
        List<String> scale = new ArrayList<>();
        for (int i = pageResult.size() - 1; i >= 0; i--) {
            SystemMonitorLog systemMonitorLog = pageResult.get(i);
            scale.add(DateUtil.formatTime(new Date(systemMonitorLog.getMonitorTime())));
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("cpu", systemMonitorLog.getOccupyCpu());
            jsonObject.put("memory", systemMonitorLog.getOccupyMemory());
            jsonObject.put("disk", systemMonitorLog.getOccupyDisk());
            series.add(jsonObject);
        }
        JSONObject object = new JSONObject();
        object.put("scales", scale);
        object.put("series", series);
        return JsonMessage.getString(200, "ok", object);
    }

    @RequestMapping(value = "exportTop")
    public void exportTop(String time) throws UnsupportedEncodingException {
        List<String> list = StrSpliter.splitTrim(time, "~", true);
        DateTime startDate = DateUtil.parseDateTime(list.get(0));
        long startTime = startDate.getTime();
        DateTime endDate = DateUtil.parseDateTime(list.get(1));
        if (startDate.equals(endDate)) {
            endDate = DateUtil.endOfDay(endDate);
        }
        long endTime = endDate.getTime();
        PageResult<SystemMonitorLog> monitorData = dbSystemMonitorLogService.getMonitorData(startTime, endTime);
        if (monitorData.getTotal() <= 0) {
            NodeForward.requestDownload(getNode(), getRequest(), getResponse(), NodeUrl.exportTop);
        } else {
            StringBuilder buf = new StringBuilder();
            buf.append("监控时间").append(",占用cpu").append(",占用内存").append(",占用磁盘").append("\r\n");
            for (SystemMonitorLog log : monitorData) {
                long monitorTime = log.getMonitorTime();
                buf.append(DateUtil.formatDateTime(DateUtil.date(monitorTime))).append(",")
                        .append(log.getOccupyCpu()).append("%").append(",")
                        .append(log.getOccupyMemory()).append("%").append(",")
                        .append(log.getOccupyDisk()).append("%").append("\r\n");
            }
            String fileName = URLEncoder.encode("Jpom系统监控", "UTF-8");
            HttpServletResponse response = getResponse();
            response.setHeader("Content-Disposition", "attachment;filename=" + new String(fileName.getBytes(StandardCharsets.UTF_8), "GBK") + ".csv");
            response.setContentType("text/csv;charset=utf-8");
            ServletUtil.write(getResponse(), buf.toString(), CharsetUtil.UTF_8);
        }
    }

    @RequestMapping(value = "processList", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String getProcessList() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.ProcessList).toString();
    }
}
