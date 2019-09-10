package io.jpom.controller;

import cn.hutool.cache.impl.CacheObj;
import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
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

import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author jiangzeyin
 * @date 2019/4/16
 */
@RestController
public class WelcomeController extends AbstractController {

    @RequestMapping(value = "getTop", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getTop(String type) {
        TimedCache<String, JSONObject> topMonitor = TopManager.getTopMonitor(type);
        Iterator<CacheObj<String, JSONObject>> cacheObjIterator = topMonitor.cacheObjIterator();
        String lastTime = "";
        List<JSONObject> array = new ArrayList<>();
        List<String> scale = new ArrayList<>();
        int count = 60;
        if ("hour".equals(type)) {
            count = 96;
        }
        JSONObject value = null;
        int minSize = 12;
        while (cacheObjIterator.hasNext()) {
            CacheObj<String, JSONObject> cacheObj = cacheObjIterator.next();
            String key = cacheObj.getKey();
            if (StrUtil.isNotEmpty(lastTime)) {
                String nextScaleTime = getNextScaleTime(lastTime, type);
                if (!key.equals(nextScaleTime)) {
                    filling(lastTime, key, type, array, scale, count);
                }
            }
            lastTime = key;
            scale.add(key);
            value = cacheObj.getValue();
            array.add(value);
        }
        if (value != null) {
            String time = value.getString("time");
            String nowNextScale = getNowNextScale(type);
            String nextScaleTime = getNextScaleTime(time, type);
            if (!nextScaleTime.equals(nowNextScale)) {
                filling(nextScaleTime, nowNextScale, type, array, scale, count);
            }
        }
        //限定数组最大数量
        if (array.size() > count) {
            array = array.subList(array.size() - count, array.size());
            scale = scale.subList(scale.size() - count, scale.size());
        }
        while (scale.size() <= minSize) {
            if (scale.size() == 0) {
                scale.add(getNowNextScale(type));
            }
            String time = scale.get(scale.size() - 1);
            String newTime = getNextScaleTime(time, type);
            scale.add(newTime);
        }
        JSONObject object = new JSONObject();
        object.put("scale", scale);
        object.put("series", array);
        object.put("maxSize", count);
        return JsonMessage.getString(200, "", object);
    }

    /**
     * 补空，将断开的监控填空
     */
    private void filling(String startTime, String endTime, String type, List<JSONObject> data, List<String> scale, int maxSize) {
        for (int i = 0; i <= maxSize; i++) {
            String nextScaleTime = getNextScaleTime(startTime, type);
            if (nextScaleTime.equals(endTime)) {
                return;
            }
            JSONObject object = new JSONObject();
            object.put("time", nextScaleTime);
            data.add(object);
            startTime = nextScaleTime;
            scale.add(nextScaleTime);
            if (i == maxSize) {
                data.clear();
                scale.clear();
            }
        }
    }

    /**
     * 当前时间的下一个刻度
     *
     * @return String
     */
    private String getNowNextScale(String type) {
        DateTime date = DateUtil.date();
        if ("hour".equals(type)) {
            int minute = date.minute();
            int hour = date.hour(true);
            String minuteStr = "00:00";
            if (minute > 15) {
                int i = minute % 15;
                minuteStr = i * 15 + ":00";
            }
            return getNextScaleTime(hour + ":" + minuteStr, type);
        }
        int second = date.second();
        String secondStr = second >= 30 ? "30" : "00";
        String format = DateUtil.format(date, "HH:mm");
        return getNextScaleTime(format + ":" + secondStr, type);
    }

    /**
     * 指定时间的下一个刻度
     *
     * @return String
     */
    private String getNextScaleTime(String time, String type) {
        DateTime dateTime = DateUtil.parseTime(time);
        DateTime newTime;
        if ("hour".equals(type)) {
            newTime = dateTime.offsetNew(DateField.MINUTE, 15);
        } else {
            newTime = dateTime.offsetNew(DateField.SECOND, 30);
        }
        return DateUtil.formatTime(newTime);
    }

    /**
     * 导出
     */
    @RequestMapping(value = "exportTop")
    public String exportTop(String type) throws Exception {
        ExcelWriter writer = ExcelUtil.getBigWriter();
        TimedCache<String, JSONObject> topMonitor = TopManager.getTopMonitor(type);
        Iterator<CacheObj<String, JSONObject>> cacheObjIterator = topMonitor.cacheObjIterator();
        if (topMonitor.size() <= 0) {
            return "暂无监控数据";
        }
        int index = 1;
        writer.writeCellValue(0, 0, "监控时间");
        writer.writeCellValue(1, 0, "占用cpu");
        writer.writeCellValue(2, 0, "占用内存");
        writer.writeCellValue(3, 0, "占用磁盘");
        while (cacheObjIterator.hasNext()) {
            CacheObj<String, JSONObject> next = cacheObjIterator.next();
            String time = next.getKey();
            JSONObject value = next.getValue();
            writer.writeCellValue(0, index, time);
            writer.writeCellValue(1, index, value.getString("cpu") + "%");
            writer.writeCellValue(2, index, value.getString("memory") + "%");
            writer.writeCellValue(3, index, value.getString("disk") + "%");
            index++;
        }
        String fileName = URLEncoder.encode("Jpom系统监控", "UTF-8");
        HttpServletResponse response = getResponse();
        response.setHeader("Content-Disposition", "attachment;filename=" + new String(fileName.getBytes(StandardCharsets.UTF_8), "GBK") + ".xlsx");
        response.setContentType("application/vnd.ms-excel;charset=utf-8");
        writer.flush(response.getOutputStream());
        return "";
    }

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
