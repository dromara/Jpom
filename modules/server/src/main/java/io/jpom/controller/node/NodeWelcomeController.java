package io.jpom.controller.node;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.text.StrSplitter;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
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
import io.jpom.model.data.UserModel;
import io.jpom.model.log.SystemMonitorLog;
import io.jpom.service.dblog.DbSystemMonitorLogService;
import io.jpom.util.StringUtil;
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

    private long getCycleMillis() {
        Cycle cycle = getCycle();
        long millis = cycle == null ? TimeUnit.SECONDS.toMillis(30) : cycle.getMillis();
        if (millis <= 0) {
            millis = TimeUnit.SECONDS.toMillis(30);
        }
        return millis;
    }

//    @RequestMapping(value = "welcome", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
//    public String welcome() {
//        Cycle cycle = getCycle();
//        long millis = getCycleMillis();
//        if (cycle != null && cycle != Cycle.none) {
//            //
//            setAttribute("monitorCycle", true);
//        }
//        setAttribute("cycleTime", millis);
//        return "node/welcome";
//    }

//    @RequestMapping(value = "nodeMonitor.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
//    public String nodeMonitor() {
//        return "node/nodeMonitor";
//    }

    @RequestMapping(value = "nodeMonitor_data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String nodeMonitorJson(String time) {
        JSONObject object = getData(time);
        return JsonMessage.getString(200, "ok", object);
    }

    private PageResult<SystemMonitorLog> getList(String time, long millis) {
        long endTime = System.currentTimeMillis();
        long startTime = endTime - TimeUnit.MINUTES.toMillis(30);
        if (StrUtil.isNotEmpty(time)) {
            //  处理时间
            List<String> list = StrSplitter.splitTrim(time, "~", true);
            DateTime startDate = DateUtil.parseDateTime(list.get(0));
            startTime = startDate.getTime();
            DateTime endDate = DateUtil.parseDateTime(list.get(1));
            if (startDate.equals(endDate) || StrUtil.equalsAny("00:00:00", endDate.toString(DatePattern.NORM_TIME_FORMAT), startDate.toString(DatePattern.NORM_TIME_FORMAT))) {
                endDate = DateUtil.endOfDay(endDate);
            }
            endTime = endDate.getTime();
        }
        int count = (int) ((endTime - startTime) / millis);
        NodeModel node = getNode();
        // 开启了节点信息采集
        Page pageObj = new Page(1, count);
        pageObj.addOrder(new Order("monitorTime", Direction.DESC));
        Entity entity = Entity.create();
        entity.set("nodeId", node.getId());

        entity.set(" MONITORTIME", ">= " + startTime);
        entity.set("MONITORTIME", "<= " + endTime);
        return dbSystemMonitorLogService.listPage(entity, pageObj);
    }

    private JSONObject getData(String selTime) {
        long millis = getCycleMillis();
        PageResult<SystemMonitorLog> pageResult = getList(selTime, millis);
        List<JSONObject> series = new ArrayList<>();
        List<String> scale = new ArrayList<>();
        for (int i = pageResult.size() - 1; i >= 0; i--) {
            SystemMonitorLog systemMonitorLog = pageResult.get(i);
            if (StrUtil.isEmpty(selTime)) {
                scale.add(DateUtil.formatTime(new Date(systemMonitorLog.getMonitorTime())));
            } else {
                scale.add(new DateTime(systemMonitorLog.getMonitorTime()).toString(DatePattern.NORM_DATETIME_PATTERN));
            }
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("cpu", systemMonitorLog.getOccupyCpu());
            jsonObject.put("memory", systemMonitorLog.getOccupyMemory());
            jsonObject.put("disk", systemMonitorLog.getOccupyDisk());
            series.add(jsonObject);
        }
        //
        int minSize = 12;
        while (scale.size() <= minSize) {
            if (scale.size() == 0) {
                scale.add(DateUtil.formatTime(DateUtil.date()));
            }
            String time = scale.get(scale.size() - 1);
            String newTime = StringUtil.getNextScaleTime(time, millis);
            scale.add(newTime);
        }

        JSONObject object = new JSONObject();
        object.put("scales", scale);
        object.put("series", series);
        return object;
    }

    @RequestMapping(value = "getTop", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String getTop() {
        Cycle cycle = getCycle();
        NodeModel node = getNode();
        if (cycle == null || cycle == Cycle.none) {
            // 未开启、直接查询
            return NodeForward.request(node, getRequest(), NodeUrl.GetTop).toString();
        }
        JSONObject object = getData(null);
        return JsonMessage.getString(200, "ok", object);
    }

    @RequestMapping(value = "exportTop")
    public void exportTop(String time) throws UnsupportedEncodingException {
        PageResult<SystemMonitorLog> monitorData = getList(time, getCycleMillis());
        if (monitorData.getTotal() <= 0) {
            //            NodeForward.requestDownload(node, getRequest(), getResponse(), NodeUrl.exportTop);
        } else {
            NodeModel node = getNode();
            StringBuilder buf = new StringBuilder();
            buf.append("监控时间").append(",占用cpu").append(",占用内存").append(",占用磁盘").append("\r\n");
            for (SystemMonitorLog log : monitorData) {
                long monitorTime = log.getMonitorTime();
                buf.append(DateUtil.date(monitorTime).toString()).append(",")
                        .append(log.getOccupyCpu()).append("%").append(",")
                        .append(log.getOccupyMemory()).append("%").append(",")
                        .append(log.getOccupyDisk()).append("%").append("\r\n");
            }
            String fileName = URLEncoder.encode("Jpom系统监控-" + node.getId(), "UTF-8");
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

    @RequestMapping(value = "kill.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String kill() {
        UserModel user = getUser();
        if (!user.isSystemUser()) {
            return JsonMessage.getString(405, "没有权限");
        }
        return NodeForward.request(getNode(), getRequest(), NodeUrl.Kill).toString();
    }
}
