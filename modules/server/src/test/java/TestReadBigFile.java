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

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.SystemClock;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.NioUtil;
import cn.hutool.core.io.unit.DataSize;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.DigestAlgorithm;
import cn.hutool.crypto.digest.Digester;
import io.jpom.util.FileUtils;
import io.jpom.util.StrictSyncFinisher;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.util.Assert;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * https://www.dandelioncloud.cn/article/details/1513013511700692994
 * https://www.cnblogs.com/yjmyzz/p/how-to-split-a-large-file-into-small-files-fastly.html
 *
 * @author bwcx_jzy
 * @since 2023/1/10
 */
@Slf4j
public class TestReadBigFile {


    private File file = new File("D:\\迅雷下载\\zh-cn_windows_11_business_editions_version_22h2_updated_sep_2022_x64_dvd_515a832b.iso");

    private File partDir = new File("D:\\test\\part");

    private File saveDir = new File("D:\\test\\");

    private File testFile = new File("D:\\test\\zh-cn_windows_11_business_editions_version_22h2_updated_sep_2022_x64_dvd_515a832b.iso");

    @Test
    public void test() throws IOException {
        long start = System.currentTimeMillis();
        FileUtil.del(partDir);
        long length = file.length();
        Assert.state(length > 0, "空文件不能上传");
        //如果小数点大于1，整数加一 例如4.1 =》5
        long chunkSize = DataSize.ofMegabytes(1).toBytes();
        int total = (int) Math.ceil((double) length / chunkSize);
        Queue<Integer> queueList = new ConcurrentLinkedDeque<>();
        for (int i = 0; i < total; i++) {
            queueList.offer(i);
        }
        String fileSize = FileUtil.readableFileSize(length);
        // 并发数
        int concurrent = 2;
        List<Integer> success = Collections.synchronizedList(new ArrayList<>(total));
        AtomicLong atomicProgressSize = new AtomicLong(0);
        // 需要计算 并发数和最大任务数，如果任务数小于并发数则使用任务数
        int threadSize = Math.min(concurrent, total);
        System.out.println("线程数：" + threadSize);
        try (StrictSyncFinisher syncFinisher = new StrictSyncFinisher(threadSize, total)) {
            Runnable runnable = () -> {
                // 取出任务
                Integer currentChunk = queueList.poll();
                if (currentChunk == null) {
                    return;
                }
                try {
                    try (FileInputStream inputStream = new FileInputStream(file)) {
                        try (FileChannel inputChannel = inputStream.getChannel()) {
                            //分配缓冲区，设定每次读的字节数
                            ByteBuffer byteBuffer = ByteBuffer.allocate((int) chunkSize);
                            // 移动到指定位置开始读取
                            inputChannel.position(currentChunk * chunkSize);
                            inputChannel.read(byteBuffer);
                            //上面把数据写入到了buffer，所以可知上面的buffer是写模式，调用flip把buffer切换到读模式，读取数据
                            byteBuffer.flip();
                            byte[] array = new byte[byteBuffer.remaining()];
                            byteBuffer.get(array, 0, array.length);
                            success.add(currentChunk);
                            File partFile = FileUtil.file(partDir, file.getName() + "." + currentChunk);
                            FileUtil.mkParentDirs(partFile);
                            FileUtil.writeBytes(array, partFile);
                            long end = Math.min(length, ((success.size() - 1) * chunkSize) + chunkSize);
                            // 保存线程安全顺序回调进度信息
                            atomicProgressSize.set(Math.max(end, atomicProgressSize.get()));
                            long size = atomicProgressSize.get();
                            log.info("{} {} {},读取文件 {} 内容花费：{}", total, fileSize, FileUtil.readableFileSize(size), NumberUtil.formatPercent(((double) size / length), 0), DateUtil.formatBetween(SystemClock.now() - start));
                        }
                    }

                } catch (Exception e) {
                    log.error("分片上传文件异常", e);
                    // 终止上传
                    queueList.clear();
                }
            };
            for (int i = 0; i < total; i++) {
                syncFinisher.addWorker(runnable);
            }
            syncFinisher.start();
        }
        log.info("读取文件内容花费：" + DateUtil.formatBetween(SystemClock.now() - start));
    }

    @Test
    public void testMerge() throws IOException {
        long start = SystemClock.now();
        File[] files = partDir.listFiles();

        long start2 = SystemClock.now();
        String fileSumMd5 = SecureUtil.sha1(file);
        log.info("解析文件耗时：{}", DateUtil.formatBetween(SystemClock.now() - start2));
        assert files != null;
        String name = files[0].getName();
        name = StrUtil.subBefore(name, StrUtil.DOT, true);
        File successFile = FileUtil.file(saveDir, name);
        FileUtil.del(successFile);
        try (FileOutputStream fileOutputStream = new FileOutputStream(successFile)) {
            try (FileChannel channel = fileOutputStream.getChannel()) {
                Arrays.stream(files).sorted((o1, o2) -> {
                    // 排序
                    Integer o1Int = Convert.toInt(FileUtil.extName(o1));
                    Integer o2Int = Convert.toInt(FileUtil.extName(o2));
                    return o1Int.compareTo(o2Int);
                }).forEach(file12 -> {
                    try {
                        FileUtils.appendChannel(file12, channel);
                        log.info("合并 {} 文件花费：{}", file12.getName(), DateUtil.formatBetween(SystemClock.now() - start));
                    } catch (Exception e) {
                        throw Lombok.sneakyThrow(e);
                    }
                });
            }
        }
        // 对比文件信息
        start2 = SystemClock.now();
        String newSha1 = SecureUtil.sha1(successFile);
        log.info("解析文件耗时：{}", DateUtil.formatBetween(SystemClock.now() - start2));
        Assert.state(StrUtil.equals(newSha1, fileSumMd5), () -> {
            log.warn("文件合并异常 {}:{} -> {}", FileUtil.getAbsolutePath(successFile), newSha1, fileSumMd5);
            return "文件合并后异常,文件不完成可能被损坏";
        });
        log.info("合并文件花费：" + DateUtil.formatBetween(SystemClock.now() - start));
    }

    @Test
    public void testMerge2() throws IOException {
        long start = SystemClock.now();
        File[] files = partDir.listFiles();
        long start2 = SystemClock.now();
        String fileSumMd5 = null;// SecureUtil.sha1(file);
        log.info("解析文件耗时：{}", DateUtil.formatBetween(SystemClock.now() - start2));
        assert files != null;
        String name = files[0].getName();
        name = StrUtil.subBefore(name, StrUtil.DOT, true);
        File successFile = FileUtil.file(saveDir, name);
        FileUtil.del(successFile);
        Digester digester = SecureUtil.sha1();
        try (OutputStream fileOutputStream = Files.newOutputStream(successFile.toPath())) {

            try (DigestOutputStream digestOutputStream = new DigestOutputStream(fileOutputStream, digester.getDigest());) {
                Arrays.stream(files).sorted((o1, o2) -> {
                    // 排序
                    Integer o1Int = Convert.toInt(FileUtil.extName(o1));
                    Integer o2Int = Convert.toInt(FileUtil.extName(o2));
                    return o1Int.compareTo(o2Int);
                }).forEach(file12 -> {
                    try {
                        digestOutputStream.write(FileUtil.readBytes(file12));
                        log.info("合并 {} 文件花费：{}", file12.getName(), DateUtil.formatBetween(SystemClock.now() - start));
                    } catch (Exception e) {
                        throw Lombok.sneakyThrow(e);
                    }
                });
            }
        }
        // 对比文件信息
        start2 = SystemClock.now();
        String newSha1 = HexUtil.encodeHexStr(digester.getDigest().digest());
        //SecureUtil.sha1(successFile);
        log.info("解析文件耗时：{}", DateUtil.formatBetween(SystemClock.now() - start2));
        Assert.state(StrUtil.equals(newSha1, fileSumMd5), () -> {
            log.warn("文件合并异常 {}:{} -> {}", FileUtil.getAbsolutePath(successFile), newSha1, fileSumMd5);
            return "文件合并后异常,文件不完成可能被损坏";
        });
        log.info("合并文件花费：" + DateUtil.formatBetween(SystemClock.now() - start));
    }

    /**
     * e0e4708889197545603b4631de1f4a90dbcb2435
     *
     * @throws FileNotFoundException
     */
    @Test
    public void testSha1() throws FileNotFoundException {
        Digester digester = new Digester(DigestAlgorithm.SHA1);
        String digestHex = digester.digestHex(new FileInputStream(file), NioUtil.DEFAULT_LARGE_BUFFER_SIZE);
        System.out.println(digestHex);
    }

    @Test
    public void testSha1_2() throws IOException, NoSuchAlgorithmException {
//        Digester digester = new Digester(DigestAlgorithm.SHA1);
        MessageDigest messagedigest = MessageDigest.getInstance("SHA-1");
        FileInputStream in = new FileInputStream(file);

        FileChannel ch = in.getChannel();

        MappedByteBuffer byteBuffer = ch.map(FileChannel.MapMode.READ_ONLY, 0, file.length());

        messagedigest.update(byteBuffer);

        String hexStr = HexUtil.encodeHexStr(messagedigest.digest());

        System.out.println(hexStr);
    }

    @Test
    public void testSha1_3() throws IOException {
        String sha1 = SecureUtil.sha1(testFile);
        System.out.println(sha1);
        Digester digester = SecureUtil.sha1();
        OutputStream is = Files.newOutputStream(testFile.toPath());
        DigestOutputStream di = new DigestOutputStream(is, digester.getDigest());
        MessageDigest messageDigest = di.getMessageDigest();
        System.out.println(HexUtil.encodeHexStr(messageDigest.digest()));
    }

    @Test
    public void compared_md5_sha1() {
        TimeInterval timeInterval = new TimeInterval();
        //
        timeInterval.start("md5");
        System.out.println(SecureUtil.md5(file));
        System.out.println(timeInterval.intervalPretty("md5"));
        //
        timeInterval.start("sha1");
        System.out.println(SecureUtil.sha1(file));
        System.out.println(timeInterval.intervalPretty("sha1"));
        //
    }
}
