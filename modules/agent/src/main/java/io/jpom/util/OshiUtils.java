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
package io.jpom.util;

import cn.hutool.core.date.SystemClock;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.oshi.CpuInfo;
import cn.hutool.system.oshi.OshiUtil;
import com.alibaba.fastjson2.JSONObject;
import oshi.hardware.GlobalMemory;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author bwcx_jzy
 * @since 2023/2/12
 */
public class OshiUtils {

    /**
     * 获取信息简单的基础状态信息
     *
     * @return json
     */
    public static JSONObject getSimpleInfo() {
        CpuInfo cpuInfo = OshiUtil.getCpuInfo();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("cpu", cpuInfo.getUsed());
        //
        GlobalMemory globalMemory = OshiUtil.getMemory();
        jsonObject.put("memory", NumberUtil.div(globalMemory.getTotal() - globalMemory.getAvailable(), globalMemory.getTotal(), 2) * 100);
        //
        FileSystem fileSystem = OshiUtil.getOs().getFileSystem();
        List<OSFileStore> fileStores = fileSystem.getFileStores();
        long total = 0, used = 0;
        for (OSFileStore fs : fileStores) {
            total += fs.getTotalSpace();
            used += (fs.getTotalSpace() - fs.getUsableSpace());
        }
        jsonObject.put("disk", NumberUtil.div(used, total, 2) * 100);
        jsonObject.put("time", SystemClock.now());
        return jsonObject;
    }

    /**
     * 查询进程列表
     *
     * @param name  进程名称
     * @param count 数量
     * @return list
     */
    public static List<JSONObject> getProcesses(String name, int count) {
        OperatingSystem operatingSystem = OshiUtil.getOs();
        return operatingSystem.getProcesses(
                osProcess -> StrUtil.equalsIgnoreCase(osProcess.getName(), name),
                OperatingSystem.ProcessSorting.CPU_DESC,
                count)
            .stream()
            .map(osProcess -> {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("processCpuLoadCumulative", osProcess.getProcessCpuLoadCumulative());
                jsonObject.put("hardOpenFileLimit", osProcess.getHardOpenFileLimit());
                jsonObject.put("softOpenFileLimit", osProcess.getSoftOpenFileLimit());
                jsonObject.put("openFiles", osProcess.getOpenFiles());
                jsonObject.put("processId", osProcess.getProcessID());
                jsonObject.put("bytesWritten", osProcess.getBytesWritten());
                jsonObject.put("bytesRead", osProcess.getBytesRead());
                jsonObject.put("startTime", osProcess.getStartTime());
                jsonObject.put("upTime", osProcess.getUpTime());
                jsonObject.put("userTime", osProcess.getUpTime());
                jsonObject.put("user", osProcess.getUser());
                jsonObject.put("state", osProcess.getState());
                jsonObject.put("name", osProcess.getName());
                jsonObject.put("virtualSize", osProcess.getVirtualSize());
                jsonObject.put("commandLine", osProcess.getCommandLine());
                jsonObject.put("priority", osProcess.getPriority());
                jsonObject.put("path", osProcess.getPath());
                jsonObject.put("residentSetSize", osProcess.getResidentSetSize());
                return jsonObject;
            })
            .collect(Collectors.toList());
    }
}
