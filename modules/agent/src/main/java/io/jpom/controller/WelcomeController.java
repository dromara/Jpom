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
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.SystemClock;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.controller.base.AbstractController;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.JpomManifest;
import io.jpom.common.commander.AbstractSystemCommander;
import io.jpom.model.system.ProcessModel;
import io.jpom.system.TopManager;
import io.jpom.util.StringUtil;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * @author jiangzeyin
 * @since 2019/4/16
 */
@RestController
public class WelcomeController extends AbstractController {

    @PostMapping(value = "getDirectTop", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getDirectTop() {
        JSONObject topInfo = AbstractSystemCommander.getInstance().getAllMonitor();
        //
        topInfo.put("time", SystemClock.now());
        return JsonMessage.getString(200, "ok", topInfo);
    }

    @RequestMapping(value = "getTop", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
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
                scale.add(DateUtil.formatTime(DateUtil.date()));
            }
            String time = scale.get(scale.size() - 1);
            String newTime = StringUtil.getNextScaleTime(time, millis);
            scale.add(newTime);
        }
        JSONObject object = new JSONObject();
        object.put("scales", scale);
        object.put("series", series);
        return JsonMessage.getString(200, "", object);
    }


    @RequestMapping(value = "processList", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String getProcessList(String processName) {
        processName = StrUtil.emptyToDefault(processName, "java");
        List<ProcessModel> array = AbstractSystemCommander.getInstance().getProcessList(processName);
        if (array != null && !array.isEmpty()) {
            array.sort(Comparator.comparingInt(ProcessModel::getPid));
            return JsonMessage.getString(200, "", array);
        }
        return JsonMessage.getString(402, "没有获取到进程信息");
    }


    @RequestMapping(value = "kill.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String kill(int pid) {
        long jpomAgentId = JpomManifest.getInstance().getPid();
        Assert.state(!StrUtil.equals(StrUtil.toString(jpomAgentId), StrUtil.toString(pid)), "不支持在线关闭 Agent 进程");
        String result = AbstractSystemCommander.getInstance().kill(null, pid);
        if (StrUtil.isEmpty(result)) {
            result = "成功kill";
        }
        return JsonMessage.getString(200, result);
    }
}
