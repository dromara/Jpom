package io.jpom.controller;

import cn.hutool.cache.impl.CacheObj;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.controller.base.AbstractController;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.commander.AbstractSystemCommander;
import io.jpom.model.system.ProcessModel;
import io.jpom.system.TopManager;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author jiangzeyin
 * @date 2019/4/16
 */
@RestController
public class WelcomeController extends AbstractController {

    @RequestMapping(value = "getDirectTop", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getDirectTop() {
        JSONObject topInfo = AbstractSystemCommander.getInstance().getAllMonitor();
        //
        topInfo.put("time", System.currentTimeMillis());
        return JsonMessage.getString(200, "ok", topInfo);
    }

    @RequestMapping(value = "getTop", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getTop(Long millis) {
        Iterator<CacheObj<String, JSONObject>> cacheObjIterator = TopManager.get();
        List<JSONObject> series = new ArrayList<>();
        List<String> scale = new ArrayList<>();
        int count = 60;
        int minSize = 12;
        while (cacheObjIterator.hasNext()) {
            CacheObj<String, JSONObject> cacheObj = cacheObjIterator.next();
            String key = cacheObj.getKey();
            scale.add(key);
            JSONObject value = cacheObj.getValue();
            series.add(value);
        }
        //限定数组最大数量
        if (series.size() > count) {
            series = series.subList(series.size() - count, series.size());
            scale = scale.subList(scale.size() - count, scale.size());
        }
        while (scale.size() <= minSize) {
            if (scale.size() == 0) {
                scale.add(getNowNextScale());
            }
            String time = scale.get(scale.size() - 1);
            String newTime = getNextScaleTime(time, millis);
            scale.add(newTime);
        }
        JSONObject object = new JSONObject();
        object.put("scales", scale);
        object.put("series", series);
        object.put("maxSize", count);
        return JsonMessage.getString(200, "", object);
    }

    /**
     * 当前时间的下一个刻度
     *
     * @return String
     */
    private String getNowNextScale() {
        return DateUtil.formatTime(DateUtil.date());
    }

    /**
     * 指定时间的下一个刻度
     *
     * @return String
     */
    private String getNextScaleTime(String time, Long millis) {
        DateTime dateTime = DateUtil.parseTime(time);
        if (millis == null) {
            millis = 30 * 1000L;
        }
        DateTime newTime = dateTime.offsetNew(DateField.SECOND, (int) (millis / 1000));
        return DateUtil.formatTime(newTime);
    }

//    /**
//     * 导出
//     */
//    @RequestMapping(value = "exportTop")
//    public String exportTop() throws Exception {
//        TimedCache<String, JSONObject> topMonitor = TopManager.getTopMonitor();
//        Iterator<CacheObj<String, JSONObject>> cacheObjIterator = topMonitor.cacheObjIterator();
//        if (topMonitor.size() <= 0) {
//            return "暂无监控数据";
//        }
//        StringBuilder buf = new StringBuilder();
//        buf.append("监控时间").append(",占用cpu").append(",占用内存").append(",占用磁盘").append("\r\n");
//        while (cacheObjIterator.hasNext()) {
//            CacheObj<String, JSONObject> next = cacheObjIterator.next();
//            JSONObject value = next.getValue();
//            long monitorTime = value.getLongValue("monitorTime");
//            buf.append(DateUtil.formatDateTime(DateUtil.date(monitorTime)));
//            buf.append(",").append(value.getString("cpu")).append("%");
//            buf.append(",").append(value.getString("memory")).append("%");
//            buf.append(",").append(value.getString("disk")).append("%");
//            buf.append("\r\n");
//        }
//        String fileName = URLEncoder.encode("Jpom系统监控", "UTF-8");
//        HttpServletResponse response = getResponse();
//        response.setHeader("Content-Disposition", "attachment;filename=" + new String(fileName.getBytes(StandardCharsets.UTF_8), "GBK") + ".csv");
//        response.setContentType("text/csv;charset=utf-8");
//        ServletUtil.write(getResponse(), buf.toString(), CharsetUtil.UTF_8);
//        return "";
//    }

    @RequestMapping(value = "processList", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String getProcessList() {
        List<ProcessModel> array = AbstractSystemCommander.getInstance().getProcessList();
        if (array != null && !array.isEmpty()) {
            return JsonMessage.getString(200, "", array);
        }
        return JsonMessage.getString(402, "没有获取到进程信息");
    }
}
