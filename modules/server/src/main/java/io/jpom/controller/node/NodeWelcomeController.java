package io.jpom.controller.node;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.db.PageResult;
import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.JsonMessage;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.data.UserModel;
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

    @RequestMapping(value = "welcome", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String welcome() {
        return "node/welcome";
    }

    @RequestMapping(value = "getTop", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String getTop(String type) {
//        UserModel userModel = getUserModel();
//        dbSystemMonitorLogService.init(userModel);
        JSONObject topMonitor = dbSystemMonitorLogService.getTopMonitor(type);
        if (topMonitor != null) {
            return JsonMessage.getString(200, "", topMonitor);
        }
        return NodeForward.request(getNode(), getRequest(), NodeUrl.GetTop).toString();
    }

    @RequestMapping(value = "exportTop")
    public void exportTop(String type) throws UnsupportedEncodingException {
        PageResult<SystemMonitorLog> monitorData = dbSystemMonitorLogService.getMonitorData(type);
        if (monitorData.getTotal() <= 0) {
//            throw new RuntimeException("暂无数据");
            NodeForward.requestDownload(getNode(), getRequest(), getResponse(), NodeUrl.exportTop);
        }
        StringBuilder buf = new StringBuilder();
        buf.append("监控时间").append(",占用cpu").append(",占用内存").append(",占用磁盘").append("\r\n");
        for (SystemMonitorLog log : monitorData) {
            buf.append(log.getMonitorTime()).append(",")
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

    @RequestMapping(value = "processList", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String getProcessList() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.ProcessList).toString();
    }
}
