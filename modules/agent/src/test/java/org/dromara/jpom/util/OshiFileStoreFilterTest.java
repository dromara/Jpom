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

import org.junit.Assert;
import org.junit.Test;
import oshi.software.os.OSFileStore;

import java.util.ArrayList;
import java.util.List;

/**
 * 文件系统统计过滤规则测试（与 Go 插件端 localFilesystemTypes 同口径）。
 * <p>
 * 覆盖两类会被放大的缺陷：
 * <ol>
 *     <li>macOS：APFS 同一容器多个卷 statvfs 都返回容器总容量（实测 494.38 GB 被算成 2.70 TB）；</li>
 *     <li>Linux：tmpfs 与 cifs/nfs 等非本地存储被累加（实测 1.27 TB 真实磁盘被算成 17.65 TB）。</li>
 * </ol>
 *
 * @author Jpom
 * @since 2.11.14.6
 */
public class OshiFileStoreFilterTest {

    /**
     * 构造最小可用的 OSFileStore 夹具（只需要 mount/type/totalSpace 参与统计）。
     */
    private static OSFileStore store(String mount, String type, long totalSpace) {
        return new OshiTestFileStore(mount, type, totalSpace);
    }

    @Test
    public void testMacOsApfsContainerVolumesExcluded() {
        // 同一 APFS 容器下的伪卷，statvfs 返回的都是容器总容量，必须剔除
        Assert.assertFalse(OshiUtils.shouldCountFileStore("macOS", "/System/Volumes/Data", "apfs"));
        Assert.assertFalse(OshiUtils.shouldCountFileStore("macOS", "/System/Volumes/Preboot", "apfs"));
        Assert.assertFalse(OshiUtils.shouldCountFileStore("macOS", "/System/Volumes/Update", "apfs"));
        Assert.assertFalse(OshiUtils.shouldCountFileStore("macOS", "/System/Volumes/VM", "apfs"));
        Assert.assertFalse(OshiUtils.shouldCountFileStore("macOS", "/System/Volumes", "apfs"));
        // 历史 OSHI 家族字符串同样生效（前缀判断，不做 equals）
        Assert.assertFalse(OshiUtils.shouldCountFileStore("Mac OS X", "/System/Volumes/Data", "apfs"));
    }

    @Test
    public void testMacOsRootAndExternalVolumesKept() {
        Assert.assertTrue(OshiUtils.shouldCountFileStore("macOS", "/", "apfs"));
        Assert.assertTrue(OshiUtils.shouldCountFileStore("macOS", "/Volumes/Backup", "apfs"));
        // 尾部斜杠归一
        Assert.assertTrue(OshiUtils.shouldCountFileStore("macOS", "/Volumes/Backup/", "apfs"));
        Assert.assertTrue(OshiUtils.shouldCountFileStore("macOS", "/Volumes/Win", "NTFS"));
    }

    @Test
    public void testMacOsNetworkVolumesExcluded() {
        // 网络盘不是本地存储，与 Go 插件端 Linux 白名单口径一致
        Assert.assertFalse(OshiUtils.shouldCountFileStore("macOS", "/Volumes/Share", "smbfs"));
        Assert.assertFalse(OshiUtils.shouldCountFileStore("macOS", "/Volumes/Nfs", "nfs"));
        Assert.assertFalse(OshiUtils.shouldCountFileStore("macOS", "/Volumes/Webdav", "webdav"));
    }

    @Test
    public void testPseudoFileSystemsExcluded() {
        Assert.assertFalse(OshiUtils.shouldCountFileStore("macOS", "/dev", "devfs"));
        Assert.assertFalse(OshiUtils.shouldCountFileStore("macOS", "/net", "autofs"));
    }

    @Test
    public void testLinuxNonLocalFileSystemsExcluded() {
        // Linux：tmpfs（内存）、网络盘、snap 的 squashfs 都会让「硬盘」总量失真
        Assert.assertFalse(OshiUtils.shouldCountFileStore("Ubuntu", "/run", "tmpfs"));
        Assert.assertFalse(OshiUtils.shouldCountFileStore("Ubuntu", "/tmp", "tmpfs"));
        Assert.assertFalse(OshiUtils.shouldCountFileStore("Ubuntu", "/mnt/nas", "cifs"));
        Assert.assertFalse(OshiUtils.shouldCountFileStore("Ubuntu", "/mnt/nfs", "nfs"));
        Assert.assertFalse(OshiUtils.shouldCountFileStore("Ubuntu", "/snap/core/1", "squashfs"));
        Assert.assertFalse(OshiUtils.shouldCountFileStore("Ubuntu", "/var/lib/docker", "overlay"));
        // 真空闲数据盘仍要计入
        Assert.assertTrue(OshiUtils.shouldCountFileStore("Ubuntu", "/", "ext4"));
        Assert.assertTrue(OshiUtils.shouldCountFileStore("Ubuntu", "/data", "xfs"));
        Assert.assertTrue(OshiUtils.shouldCountFileStore("Linux", "/System/Volumes/Data", "ext4"));
    }

    @Test
    public void testWindowsKeepsAllFixedDisks() {
        // Windows 固定盘全部保留；RAW/断连映射盘的文件系统类型可能为空，不能按类型丢弃
        Assert.assertTrue(OshiUtils.shouldCountFileStore("Windows 11", "C:\\", "NTFS"));
        Assert.assertTrue(OshiUtils.shouldCountFileStore("Windows 10", "D:\\", "NTFS"));
        Assert.assertTrue(OshiUtils.shouldCountFileStore("Windows 11", "C:\\", ""));
        // 空挂载点仍然剔除
        Assert.assertFalse(OshiUtils.shouldCountFileStore("Windows 11", "", "NTFS"));
    }

    @Test
    public void testFilterFileStoresMacOsContainerCountedOnce() {
        final long volume = 494384795648L;
        List<OSFileStore> raw = new ArrayList<>();
        raw.add(store("/", "apfs", volume));
        raw.add(store("/System/Volumes/Data", "apfs", volume));
        raw.add(store("/System/Volumes/Preboot", "apfs", volume));
        raw.add(store("/System/Volumes/VM", "apfs", volume));
        raw.add(store("/dev", "devfs", 0));
        raw.add(store("/Volumes/Backup", "apfs", 2000398934016L));
        long naive = 0;
        for (OSFileStore item : raw) {
            naive += item.getTotalSpace();
        }
        Assert.assertEquals("夹具应能复现放大现象", volume * 4 + 2000398934016L, naive);

        List<OSFileStore> filtered = OshiUtils.filterFileStores(raw, "macOS");
        Assert.assertEquals("过滤后只应保留根卷与外接卷", 2, filtered.size());
        long total = 0;
        for (OSFileStore item : filtered) {
            total += item.getTotalSpace();
        }
        Assert.assertEquals(volume + 2000398934016L, total);
    }

    @Test
    public void testFilterFileStoresLinuxExcludesTmpfsAndCifs() {
        List<OSFileStore> raw = new ArrayList<>();
        raw.add(store("/", "ext4", 1272093728768L));
        raw.add(store("/tmp", "tmpfs", 16300000000L));
        raw.add(store("/mnt/a", "cifs", 4000000000000L));
        raw.add(store("/mnt/b", "cifs", 4000000000000L));
        List<OSFileStore> filtered = OshiUtils.filterFileStores(raw, "Ubuntu");
        Assert.assertEquals(1, filtered.size());
        Assert.assertEquals(1272093728768L, filtered.get(0).getTotalSpace());
    }

    @Test
    public void testFilterFileStoresDedupByMount() {
        List<OSFileStore> raw = new ArrayList<>();
        raw.add(store("/data", "ext4", 100));
        raw.add(store("/data/", "ext4", 100));
        Assert.assertEquals(1, OshiUtils.filterFileStores(raw, "Ubuntu").size());
        // Windows 盘符大小写不敏感，C:\ 与 c:\ 是同一块盘
        List<OSFileStore> win = new ArrayList<>();
        win.add(store("C:\\", "NTFS", 100));
        win.add(store("c:\\", "NTFS", 100));
        Assert.assertEquals(1, OshiUtils.filterFileStores(win, "Windows 11").size());
    }

    /**
     * 统计调用点守卫：getSystemInfo/getSimpleInfo/fileStores 三处必须与过滤后的列表同源。
     * <p>
     * 若把任一调用点改回 {@code os.getFileSystem().getFileStores()}（未过滤），
     * 在本机存在 tmpfs/cifs 时数值会与过滤结果不一致，本测试即失败。
     */
    @Test
    public void testAggregationsUseFilteredFileStores() {
        List<OSFileStore> filtered = OshiUtils.getStatFileStores();
        long total = 0;
        for (OSFileStore item : filtered) {
            total += item.getTotalSpace();
        }
        Number systemInfoTotal = (Number) OshiUtils.getSystemInfo().get("osFileStoreTotal");
        Assert.assertNotNull("getSystemInfo 必须输出 osFileStoreTotal", systemInfoTotal);
        Assert.assertEquals("getSystemInfo 的硬盘总量必须来自过滤后的文件系统列表",
            total, systemInfoTotal.longValue());
        Assert.assertEquals("fileStores 列表必须与过滤结果条数一致", filtered.size(), OshiUtils.fileStores().size());
    }

    /**
     * 测试用最小 OSFileStore 实现（只实现统计用到的 getter）。
     */
    private static final class OshiTestFileStore implements OSFileStore {
        private final String mount;
        private final String type;
        private final long totalSpace;

        private OshiTestFileStore(String mount, String type, long totalSpace) {
            this.mount = mount;
            this.type = type;
            this.totalSpace = totalSpace;
        }

        @Override
        public String getName() {
            return mount;
        }

        @Override
        public String getVolume() {
            return mount;
        }

        @Override
        public String getLabel() {
            return "";
        }

        @Override
        public String getLogicalVolume() {
            return "";
        }

        @Override
        public String getMount() {
            return mount;
        }

        @Override
        public String getDescription() {
            return "";
        }

        @Override
        public String getType() {
            return type;
        }

        @Override
        public String getOptions() {
            return "";
        }

        @Override
        public String getUUID() {
            return "";
        }

        @Override
        public long getFreeSpace() {
            return 0;
        }

        @Override
        public long getUsableSpace() {
            return 0;
        }

        @Override
        public long getTotalSpace() {
            return totalSpace;
        }

        @Override
        public long getFreeInodes() {
            return 0;
        }

        @Override
        public long getTotalInodes() {
            return 0;
        }

        @Override
        public boolean updateAttributes() {
            return true;
        }
    }
}
