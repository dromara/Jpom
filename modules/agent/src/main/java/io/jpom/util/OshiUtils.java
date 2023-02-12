package io.jpom.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.system.oshi.OshiUtil;
import com.alibaba.fastjson2.JSONObject;
import oshi.software.os.OperatingSystem;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author bwcx_jzy
 * @since 2023/2/12
 */
public class OshiUtils {

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
