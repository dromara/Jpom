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
package org.dromara.jpom.util;

import cn.hutool.core.date.SystemClock;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.oshi.CpuInfo;
import cn.hutool.system.oshi.OshiUtil;
import com.alibaba.fastjson2.JSONObject;
import lombok.Data;
import oshi.hardware.*;
import oshi.software.os.FileSystem;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;
import oshi.util.GlobalConfig;
import oshi.util.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author bwcx_jzy
 * @since 2023/2/12
 */
public class OshiUtils {

    public static final int NET_STAT_SLEEP = 1000;
    public static final int CPU_STAT_SLEEP = 1000;

    static {
        // 解决Oshi获取CPU使用率与Windows任务管理器显示不匹配的问题
        GlobalConfig.set(GlobalConfig.OSHI_OS_WINDOWS_CPU_UTILITY, true);
    }

    /**
     * 获取系统信息
     *
     * @return json
     */
    public static JSONObject getSystemInfo() {
        JSONObject jsonObject = new JSONObject();
        OperatingSystem os = OshiUtil.getOs();
        jsonObject.put("systemUptime", os.getSystemUptime());
        String manufacturer = os.getManufacturer();
        String family = os.getFamily();
        OperatingSystem.OSVersionInfo versionInfo = os.getVersionInfo();
        String versionStr = versionInfo.toString();
        jsonObject.put("osVersion", StrUtil.format("{} {} {}", manufacturer, family, versionStr));
        NetworkParams networkParams = os.getNetworkParams();
        String hostName = networkParams.getHostName();
        jsonObject.put("hostName", hostName);
        //
        HardwareAbstractionLayer hardware = OshiUtil.getHardware();
        ComputerSystem computerSystem = hardware.getComputerSystem();
        jsonObject.put("hardwareVersion", StrUtil.format("{} {}", computerSystem.getManufacturer(), computerSystem.getModel()));
        List<NetworkIF> networks = hardware.getNetworkIFs();
        List<String> collect = networks.stream()
            .flatMap((Function<NetworkIF, Stream<String>>) network -> Arrays.stream(network.getIPv4addr()))
            .collect(Collectors.toList());
        jsonObject.put("hostIpv4s", collect);
        //
        CentralProcessor processor = OshiUtil.getProcessor();
        CentralProcessor.ProcessorIdentifier identifier = processor.getProcessorIdentifier();
        jsonObject.put("osCpuIdentifierName", identifier.getName());
        jsonObject.put("osCpuCores", processor.getLogicalProcessorCount());
        GlobalMemory globalMemory = OshiUtil.getMemory();
        jsonObject.put("osMoneyTotal", globalMemory.getTotal());
        VirtualMemory virtualMemory = globalMemory.getVirtualMemory();
        jsonObject.put("osSwapTotal", virtualMemory.getSwapTotal());
        jsonObject.put("osVirtualMax", virtualMemory.getVirtualMax());
        double[] systemLoadAverage = processor.getSystemLoadAverage(3);
        jsonObject.put("osLoadAverage", systemLoadAverage);
        FileSystem fileSystem = os.getFileSystem();
        List<OSFileStore> fileStores = fileSystem.getFileStores();
        long total = fileStores.stream().mapToLong(OSFileStore::getTotalSpace).sum();
        jsonObject.put("osFileStoreTotal", total);
        //
        return jsonObject;
    }

    /**
     * 获取信息简单的基础状态信息
     *
     * @return json
     */
    public static JSONObject getSimpleInfo() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("time", SystemClock.now());
        CpuInfo cpuInfo = OshiUtil.getCpuInfo(CPU_STAT_SLEEP);
        jsonObject.put("cpu", cpuInfo.getUsed());
        //
        GlobalMemory globalMemory = OshiUtil.getMemory();
        // 在不使用交换空间的情况下，启动一个新的应用最大可用内存的大小，
        // 计算方式：MemFree+Active(file)+Inactive(file)-(watermark+min(watermark,Active(file)+Inactive(file)/2))
        // https://langzi989.github.io/2016/12/19/%E9%80%9A%E8%BF%87-proc-meminfo%E5%AE%9E%E6%97%B6%E8%8E%B7%E5%8F%96%E7%B3%BB%E7%BB%9F%E5%86%85%E5%AD%98%E4%BD%BF%E7%94%A8%E6%83%85%E5%86%B5/
        // https://www.cnblogs.com/aalan/p/17026258.html
        // https://lotabout.me/2021/Linux-Available-Memory/
        jsonObject.put("memory", NumberUtil.div(globalMemory.getTotal() - globalMemory.getAvailable(), globalMemory.getTotal(), 2) * 100);
        VirtualMemory virtualMemory = globalMemory.getVirtualMemory();
        long swapTotal = virtualMemory.getSwapTotal();
        if (swapTotal > 0) {
            jsonObject.put("swapMemory", NumberUtil.div(virtualMemory.getSwapUsed(), swapTotal, 2) * 100);
        }
        long virtualMax = virtualMemory.getVirtualMax();
        if (virtualMax > 0) {
            jsonObject.put("virtualMemory", NumberUtil.div(virtualMemory.getVirtualInUse(), virtualMax, 2) * 100);
        }
        //
        FileSystem fileSystem = OshiUtil.getOs().getFileSystem();
        List<OSFileStore> fileStores = fileSystem.getFileStores();
        long total = 0, used = 0;
        for (OSFileStore fs : fileStores) {
            total += fs.getTotalSpace();
            used += (fs.getTotalSpace() - fs.getUsableSpace());
        }
        jsonObject.put("disk", NumberUtil.div(used, total, 2) * 100);
        //
        NetIoInfo startNetInfo = getNetInfo();
        //暂停1秒
        Util.sleep(NET_STAT_SLEEP);
        NetIoInfo endNetInfo = getNetInfo();
        jsonObject.put("netTxBytes", endNetInfo.getTxbyt() - startNetInfo.getTxbyt());
        jsonObject.put("netRxBytes", endNetInfo.getRxbyt() - startNetInfo.getRxbyt());
        return jsonObject;
    }

    private static NetIoInfo getNetInfo() {
        //
        long rxBytesBegin = 0;
        long txBytesBegin = 0;
        long rxPacketsBegin = 0;
        long txPacketsBegin = 0;
        List<NetworkIF> listBegin = OshiUtil.getNetworkIFs();
        for (NetworkIF net : listBegin) {
            rxBytesBegin += net.getBytesRecv();
            txBytesBegin += net.getBytesSent();
            rxPacketsBegin += net.getPacketsRecv();
            txPacketsBegin += net.getPacketsSent();
        }
        NetIoInfo netIoInfo = new NetIoInfo();
        netIoInfo.setRxbyt(rxBytesBegin);
        netIoInfo.setTxbyt(txBytesBegin);
        netIoInfo.setRxpck(rxPacketsBegin);
        netIoInfo.setTxpck(txPacketsBegin);
        return netIoInfo;
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

    /**
     * 获取文件系统信息
     *
     * @return list
     */
    public static List<JSONObject> fileStores() {
        FileSystem fileSystem = OshiUtil.getOs().getFileSystem();
        List<OSFileStore> fileStores = fileSystem.getFileStores();
        return fileStores.stream().map(osFileStore -> {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("name", osFileStore.getName());
            jsonObject.put("volume", osFileStore.getVolume());
            jsonObject.put("logicalVolume", osFileStore.getLogicalVolume());
            jsonObject.put("mount", osFileStore.getMount());
            jsonObject.put("label", osFileStore.getLabel());
            jsonObject.put("description", osFileStore.getDescription());
            jsonObject.put("type", osFileStore.getType());
            jsonObject.put("options", osFileStore.getOptions());
            jsonObject.put("uuid", osFileStore.getUUID());
            jsonObject.put("freeInodes", osFileStore.getFreeInodes());
            jsonObject.put("totalInodes", osFileStore.getTotalInodes());
            jsonObject.put("freeSpace", osFileStore.getFreeSpace());
            jsonObject.put("totalSpace", osFileStore.getTotalSpace());
            jsonObject.put("usableSpace", osFileStore.getUsableSpace());
            return jsonObject;
        }).collect(Collectors.toList());
    }

    /**
     * 硬盘存储
     *
     * @return list
     */
    public static List<JSONObject> diskStores() {
        HardwareAbstractionLayer hardware = OshiUtil.getHardware();
        List<HWDiskStore> diskStores = hardware.getDiskStores();

        return diskStores.stream()
            .map(disk -> {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("name", disk.getName());
                jsonObject.put("model", disk.getModel());
                jsonObject.put("serial", disk.getSerial());
                jsonObject.put("size", disk.getSize());
                jsonObject.put("readBytes", disk.getReadBytes());
                jsonObject.put("reads", disk.getReads());
                jsonObject.put("writeBytes", disk.getWriteBytes());
                jsonObject.put("writes", disk.getWrites());
                jsonObject.put("currentQueueLength", disk.getCurrentQueueLength());
                jsonObject.put("transferTime", disk.getTransferTime());
                jsonObject.put("timeStamp", disk.getTimeStamp());

                List<HWPartition> partitions = disk.getPartitions();
                List<JSONObject> partition = Optional.ofNullable(partitions)
                    .map(hwPartitions -> hwPartitions.stream()
                        .map(hwPartition -> {
                            JSONObject object = new JSONObject();
                            object.put("identification", hwPartition.getIdentification());
                            object.put("name", hwPartition.getName());
                            object.put("type", hwPartition.getType());
                            object.put("major", hwPartition.getMajor());
                            object.put("minor", hwPartition.getMinor());
                            object.put("size", hwPartition.getSize());
                            object.put("mountPoint", hwPartition.getMountPoint());
                            object.put("uuid", hwPartition.getUuid());
                            return object;
                        }).collect(Collectors.toList()))
                    .orElse(new ArrayList<>());
                jsonObject.put("partition", partition);
                return jsonObject;
            })
            .collect(Collectors.toList());
    }

    public static List<JSONObject> networkInterfaces() {
        HardwareAbstractionLayer hardware = OshiUtil.getHardware();
        List<NetworkIF> networkIfs = hardware.getNetworkIFs(true);
        return Optional.ofNullable(networkIfs)
            .map(networkIfs2 -> networkIfs2.stream()
                .map(networkIf -> {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("name", networkIf.getName());
                    jsonObject.put("displayName", networkIf.getDisplayName());
                    jsonObject.put("index", networkIf.getIndex());
                    jsonObject.put("macaddr", networkIf.getMacaddr());
                    jsonObject.put("timeStamp", networkIf.getTimeStamp());
                    jsonObject.put("tnDrops", networkIf.getInDrops());
                    jsonObject.put("inErrors", networkIf.getInErrors());
                    jsonObject.put("bytesRecv", networkIf.getBytesRecv());
                    jsonObject.put("bytesSent", networkIf.getBytesSent());
                    jsonObject.put("packetsRecv", networkIf.getPacketsRecv());
                    jsonObject.put("packetsSent", networkIf.getPacketsSent());
                    jsonObject.put("collisions", networkIf.getCollisions());
                    jsonObject.put("ifAlias", networkIf.getIfAlias());
                    jsonObject.put("ifType", networkIf.getIfType());
                    jsonObject.put("ifOperStatus", networkIf.getIfOperStatus());
                    jsonObject.put("ipv4addr", networkIf.getIPv4addr());
                    jsonObject.put("ipv6addr", networkIf.getIPv6addr());
                    jsonObject.put("mtu", networkIf.getMTU());
                    jsonObject.put("outErrors", networkIf.getOutErrors());
                    jsonObject.put("speed", networkIf.getSpeed());
                    jsonObject.put("subnetMasks", networkIf.getSubnetMasks());
                    jsonObject.put("prefixLengths", networkIf.getPrefixLengths());
                    jsonObject.put("knownVmMacAddr", networkIf.isKnownVmMacAddr());
                    return jsonObject;
                }).collect(Collectors.toList()))
            .orElse(new ArrayList<>());
    }

    @Data
    private static class NetIoInfo {
        /**
         * 接收的数据包,rxpck/s
         */
        private Long rxpck;

        /**
         * 发送的数据包,txpck/s
         */
        private Long txpck;

        /**
         * 接收的KB数,rxbit/s
         */
        private Long rxbyt;

        /**
         * 发送的KB数,txbit/s
         */
        private Long txbyt;
    }
}
