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
package io.jpom.controller;

import cn.hutool.cache.impl.CacheObj;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.SystemClock;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import io.jpom.common.BaseAgentController;
import io.jpom.common.JpomManifest;
import io.jpom.common.JsonMessage;
import io.jpom.common.commander.AbstractProjectCommander;
import io.jpom.common.commander.AbstractSystemCommander;
import io.jpom.system.TopManager;
import io.jpom.util.OshiUtils;
import io.jpom.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author jiangzeyin
 * @since 2019/4/16
 */
@RestController
@Slf4j
public class WelcomeController extends BaseAgentController {

    @PostMapping(value = "getDirectTop", produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonMessage<JSONObject> getDirectTop() {
        JSONObject topInfo = AbstractSystemCommander.getInstance().getAllMonitor();
        //
        topInfo.put("time", SystemClock.now());
        return JsonMessage.success("ok", topInfo);
    }

    @RequestMapping(value = "getTop", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonMessage<JSONObject> getTop(Long millis) {
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
                scale.add(DateUtil.formatTime(DateUtil.date()));
            }
            String time = scale.get(scale.size() - 1);
            String newTime = StringUtil.getNextScaleTime(time, millis);
            scale.add(newTime);
        }
        JSONObject object = new JSONObject();
        object.put("scales", scale);
        object.put("series", series);
        return JsonMessage.success("", object);
    }


    @RequestMapping(value = "processList", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonMessage<List<JSONObject>> getProcessList(String processName, Integer count) {
        processName = StrUtil.emptyToDefault(processName, "java");
        List<JSONObject> processes = OshiUtils.getProcesses(processName, Convert.toInt(count, 20));
        processes = processes.stream().peek(jsonObject -> {
            int processId = jsonObject.getIntValue("processId");
            String port = AbstractProjectCommander.getInstance().getMainPort(processId);
            jsonObject.put("port", port);
            //
            try {
                String jpomName = AbstractProjectCommander.getInstance().getJpomNameByPid(processId);
                jsonObject.put("jpomName", jpomName);
            } catch (IOException e) {
                log.error("解析进程失败", e);
            }
        }).collect(Collectors.toList());
        return JsonMessage.success("ok", processes);
    }


    @RequestMapping(value = "kill.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonMessage<Object> kill(int pid) {
        long jpomAgentId = JpomManifest.getInstance().getPid();
        Assert.state(!StrUtil.equals(StrUtil.toString(jpomAgentId), StrUtil.toString(pid)), "不支持在线关闭 Agent 进程");
        String result = AbstractSystemCommander.getInstance().kill(null, pid);
        if (StrUtil.isEmpty(result)) {
            result = "成功kill";
        }
        return JsonMessage.success(result);
    }
}
