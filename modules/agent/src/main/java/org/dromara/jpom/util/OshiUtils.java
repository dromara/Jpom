/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.SystemClock;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.oshi.CpuInfo;
import cn.hutool.system.oshi.OshiUtil;
import com.alibaba.fastjson2.JSONObject;
import lombok.Data;
import org.dromara.jpom.configuration.MonitorConfig;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import oshi.hardware.*;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;
import oshi.util.GlobalConfig;
import oshi.util.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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

    private static final PathMatcher pathMatcher = new AntPathMatcher();

    static {
        // 解决Oshi获取CPU使用率与Windows任务管理器显示不匹配的问题
        GlobalConfig.set(GlobalConfig.OSHI_OS_WINDOWS_CPU_UTILITY, true);
    }

    /**
     * 不代表本地存储的伪文件系统（macOS 的 devfs、automount 等），统计时忽略。
     */
    private static final List<String> PSEUDO_FILE_STORE_TYPES = Arrays.asList("devfs", "autofs", "nullfs");

    /**
     * 本地存储文件系统白名单，必须与 Go 插件端 {@code localFilesystemTypes}（host_linux.go）保持一致。
     * <p>
     * 用白名单而不是黑名单：tmpfs 是内存、cifs/nfs/smbfs 是远端、squashfs（snap）互相重复，
     * 逐个累加会让「硬盘」总量远大于真实磁盘（实测 Linux 上 tmpfs + 4 个 cifs 把 1.27 TB 算成 17.65 TB），
     * 而 Go 插件端一直按白名单统计，两端口径不一致会让同一台机器在两种插件端下给出差一个数量级的「硬盘」数字，
     * 因此本列表与 {@code go-agent/internal/sysinfo/host_linux.go} 的白名单必须同步维护。
     */
    private static final Set<String> LOCAL_FILE_STORE_TYPES = new HashSet<>(Arrays.asList(
        "ext2", "ext3", "ext4", "xfs", "btrfs", "zfs", "f2fs", "jfs", "reiserfs",
        "vfat", "fat", "fat32", "ntfs", "ntfs3", "exfat", "iso9660", "udf",
        "hfs", "hfsplus", "apfs", "ufs", "zfs_member"));

    /**
     * 是否为 Windows 家族（OSHI {@link OperatingSystem#getFamily()} 取值为 {@code Windows 10/11} 等）。
     *
     * @param osFamily 操作系统家族
     * @return true 表示 Windows
     */
    static boolean isWindowsFamily(String osFamily) {
        return StrUtil.startWithIgnoreCase(StrUtil.trimToEmpty(osFamily), "win");
    }

    /**
     * 是否为 macOS 家族（OSHI 取值为 {@code macOS}，历史版本为 {@code Mac OS X}，统一按前缀判断）。
     *
     * @param osFamily 操作系统家族
     * @return true 表示 macOS
     */
    static boolean isMacFamily(String osFamily) {
        return StrUtil.startWithIgnoreCase(StrUtil.trimToEmpty(osFamily), "mac");
    }

    /**
     * 归一挂载点写法：去空白、去尾部多余斜杠（根目录保持 {@code /}）。
     *
     * @param mount 挂载点
     * @return 归一后的挂载点，空值返回空串
     */
    static String normalizeMountPoint(String mount) {
        String mountPoint = StrUtil.trimToEmpty(mount);
        while (mountPoint.length() > 1 && mountPoint.endsWith("/")) {
            mountPoint = StrUtil.subPre(mountPoint, mountPoint.length() - 1);
        }
        return mountPoint;
    }

    /**
     * 判断一个挂载点是否计入文件系统统计（总量、占用率、文件系统列表）。
     * <p>
     * 规则（与 Go 插件端同口径）：
     * <ol>
     *     <li>Windows：保持历史行为不做类型过滤（RAW、断连映射盘的文件系统类型可能为空）；
     *     <li>macOS：APFS 同一容器会挂载多个卷（/、/System/Volumes/Data、Preboot、Update、VM 等），
     *     每个卷 statvfs 返回的都是「容器总容量」，逐个累加会把同一块盘算成好几倍
     *     （实测 Mac mini M4：实际 494.38 GB 被算成 2.70 TB，即 6 个同容器卷之和），
     *     因此跳过 /System/Volumes/* 这类同容器伪卷；
     *     <li>其它平台（Linux 等）：只统计本地存储白名单内的文件系统，排除 tmpfs 与网络盘。
     * </ol>
     *
     * @param osFamily 操作系统家族 {@link OperatingSystem#getFamily()}
     * @param mount    挂载点
     * @param type     文件系统类型
     * @return true 表示计入统计
     */
    static boolean shouldCountFileStore(String osFamily, String mount, String type) {
        String mountPoint = normalizeMountPoint(mount);
        if (StrUtil.isEmpty(mountPoint)) {
            return false;
        }
        if (isWindowsFamily(osFamily)) {
            return true;
        }
        String fsType = StrUtil.trimToEmpty(type).toLowerCase();
        if (StrUtil.isEmpty(fsType) || PSEUDO_FILE_STORE_TYPES.contains(fsType)) {
            return false;
        }
        if (isMacFamily(osFamily)
            && (StrUtil.equals(mountPoint, "/System/Volumes") || StrUtil.startWith(mountPoint, "/System/Volumes/"))) {
            return false;
        }
        return LOCAL_FILE_STORE_TYPES.contains(fsType);
    }

    /**
     * 过滤并去重文件系统列表（纯函数，便于单测直接断言真实夹具）。
     *
     * @param raw      原始文件系统列表
     * @param osFamily 操作系统家族
     * @return 参与统计的文件系统
     */
    static List<OSFileStore> filterFileStores(List<OSFileStore> raw, String osFamily) {
        List<OSFileStore> result = new ArrayList<>();
        if (raw == null) {
            return result;
        }
        boolean windows = isWindowsFamily(osFamily);
        Set<String> seen = new HashSet<>();
        for (OSFileStore fileStore : raw) {
            if (!shouldCountFileStore(osFamily, fileStore.getMount(), fileStore.getType())) {
                continue;
            }
            String key = normalizeMountPoint(fileStore.getMount());
            if (windows) {
                // Windows 挂载点大小写不敏感（C:\ 与 c:\ 是同一个盘）
                key = StrUtil.replace(key, "\\", "/").toLowerCase();
            }
            if (!seen.add(key)) {
                continue;
            }
            result.add(fileStore);
        }
        return result;
    }

    /**
     * 获取参与统计的文件系统：已剔除伪文件系统、非本地存储与 macOS 同容器伪卷，并按挂载点去重。
     * 总量、占用率与文件系统列表必须共用本方法，避免三处各算一遍。
     *
     * @return 文件系统列表
     */
    public static List<OSFileStore> getStatFileStores() {
        OperatingSystem os = OshiUtil.getOs();
        return filterFileStores(os.getFileSystem().getFileStores(), os.getFamily());
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
        List<OSFileStore> fileStores = getStatFileStores();
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
    public static JSONObject getSimpleInfo(MonitorConfig monitorConfig) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("time", SystemClock.now());
        CpuInfo cpuInfo = OshiUtil.getCpuInfo(CPU_STAT_SLEEP);
        jsonObject.put("cpu", cpuInfo.getUsed());
        jsonObject.put("cpuInfo", cpuInfo);
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
        List<OSFileStore> fileStores = getStatFileStores();
        long total = 0, used = 0;
        for (OSFileStore fs : fileStores) {
            total += fs.getTotalSpace();
            used += (fs.getTotalSpace() - fs.getUsableSpace());
        }
        jsonObject.put("disk", NumberUtil.div(used, total, 2) * 100);
        //

        MonitorConfig.NetworkConfig networkConfig1 = Optional.ofNullable(monitorConfig)
            .map(MonitorConfig::getNetwork).orElse(null);
        NetIoInfo startNetInfo = getNetInfo(networkConfig1);
        //暂停1秒
        Util.sleep(NET_STAT_SLEEP);
        NetIoInfo endNetInfo = getNetInfo(networkConfig1);
        jsonObject.put("netTxBytes", endNetInfo.getTxbyt() - startNetInfo.getTxbyt());
        jsonObject.put("netRxBytes", endNetInfo.getRxbyt() - startNetInfo.getRxbyt());
        jsonObject.put("monitorIfsNames", endNetInfo.getIfsNames());
        return jsonObject;
    }

    private static NetIoInfo getNetInfo(MonitorConfig.NetworkConfig networkConfig) {
        //
        List<String> statExcludeNames = Optional.ofNullable(networkConfig)
            .map(MonitorConfig.NetworkConfig::getStatExcludeNames)
            .map(s -> StrUtil.splitTrim(s, StrUtil.COMMA))
            .orElse(CollUtil.newArrayList());
        List<String> statContainsOnlyNames = Optional.ofNullable(networkConfig)
            .map(MonitorConfig.NetworkConfig::getStatContainsOnlyNames)
            .map(s -> StrUtil.splitTrim(s, StrUtil.COMMA))
            .orElse(CollUtil.newArrayList());
        long rxBytesBegin = 0;
        long txBytesBegin = 0;
        long rxPacketsBegin = 0;
        long txPacketsBegin = 0;
        List<NetworkIF> listBegin = OshiUtil.getNetworkIFs();
        StringBuilder ifsNames = new StringBuilder(StrUtil.EMPTY);
        if (listBegin != null) {
            listBegin = listBegin.stream()
                .filter(networkIF -> CollUtil.isEmpty(statExcludeNames) || !isMatch(statExcludeNames, networkIF.getName()))
                .filter(networkIF -> CollUtil.isEmpty(statContainsOnlyNames) || isMatch(statContainsOnlyNames, networkIF.getName()))
                .collect(Collectors.toList());
            for (int i = 0; i < listBegin.size(); i++) {
                NetworkIF net = listBegin.get(i);
                rxBytesBegin += net.getBytesRecv();
                txBytesBegin += net.getBytesSent();
                rxPacketsBegin += net.getPacketsRecv();
                txPacketsBegin += net.getPacketsSent();
                ifsNames.append(i == 0 ? net.getName() : "," + net.getName());
            }
        }
        NetIoInfo netIoInfo = new NetIoInfo();
        netIoInfo.setRxbyt(rxBytesBegin);
        netIoInfo.setTxbyt(txBytesBegin);
        netIoInfo.setRxpck(rxPacketsBegin);
        netIoInfo.setTxpck(txPacketsBegin);
        netIoInfo.setIfsNames(ifsNames.toString());
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
        List<OSFileStore> fileStores = getStatFileStores();
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

    public static boolean isMatch(List<String> list, String keyword) {
        for (String pattern : list) {
            if (pathMatcher.match(pattern, keyword)) {
                return true;
            }
        }
        return false;
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
        /**
         * ifaceNames
         */
        private String ifsNames;
    }
}
