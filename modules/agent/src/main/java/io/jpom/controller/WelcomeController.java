package io.jpom.controller;

import cn.hutool.cache.impl.CacheObj;
import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
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
        int minSize = 30;
        if ("day".equals(type)) {
            count = 96;
            minSize = 24;
        }
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
            JSONObject value = cacheObj.getValue();
            array.add(value);
        }
        //限定数组最大数量
        if (array.size() > count) {
            array = array.subList(array.size() - count, array.size());
            scale = scale.subList(scale.size() - count, scale.size());
        }
        if (array.size() <= 0) {
            DateTime date = DateUtil.date();
            for (int i = 0; i < minSize; i++) {
                String time = DateUtil.formatTime(date);
                scale.add(time);
                if ("day".equals(type)) {
                    date = DateUtil.offset(date, DateField.HOUR, 1);
                } else {
                    date = DateUtil.offset(date, DateField.MINUTE, 1);
                }
            }
            JSONObject item = new JSONObject();
            item.put("cpu", 0);
            item.put("disk", 0);
            item.put("memory", 0);
            array.add(item);
        }
        JSONObject object = new JSONObject();
        object.put("scales", scale);
        object.put("series", array);
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
     * 指定时间的下一个刻度
     *
     * @return String
     */
    private String getNextScaleTime(String time, String type) {
        DateTime dateTime = DateUtil.parseTime(time);
        DateTime newTime;
        if ("day".equals(type)) {
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
        TimedCache<String, JSONObject> topMonitor = TopManager.getTopMonitor(type);
        Iterator<CacheObj<String, JSONObject>> cacheObjIterator = topMonitor.cacheObjIterator();
        if (topMonitor.size() <= 0) {
            return "暂无监控数据";
        }
        StringBuilder buf = new StringBuilder();
        buf.append("监控时间").append(",占用cpu").append(",占用内存").append(",占用磁盘").append("\r\n");
        while (cacheObjIterator.hasNext()) {
            CacheObj<String, JSONObject> next = cacheObjIterator.next();
            String time = next.getKey();
            JSONObject value = next.getValue();
            buf.append(time);
            buf.append(",").append(value.getString("cpu")).append("%");
            buf.append(",").append(value.getString("memory")).append("%");
            buf.append(",").append(value.getString("disk")).append("%");
            buf.append("\r\n");
        }
        String fileName = URLEncoder.encode("Jpom系统监控", "UTF-8");
        HttpServletResponse response = getResponse();
        response.setHeader("Content-Disposition", "attachment;filename=" + new String(fileName.getBytes(StandardCharsets.UTF_8), "GBK") + ".csv");
        response.setContentType("text/csv;charset=utf-8");
        ServletUtil.write(getResponse(), buf.toString(), CharsetUtil.UTF_8);
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
