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

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.SystemClock;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.unit.DataSize;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RuntimeUtil;
import io.jpom.util.StrictSyncFinisher;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.util.Assert;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * https://www.dandelioncloud.cn/article/details/1513013511700692994
 *
 * @author bwcx_jzy
 * @since 2023/1/10
 */
@Slf4j
public class TestReadBigFile {


    private File file = new File("D:\\迅雷下载\\zh-cn_windows_11_business_editions_version_22h2_updated_sep_2022_x64_dvd_515a832b.iso");

    @Test
    public void test() throws IOException {
        long start = System.currentTimeMillis();
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
                            long end = Math.min(length, ((success.size() - 1) * chunkSize) + chunkSize);
                            // 保存线程安全顺序回调进度信息
                            atomicProgressSize.set(Math.max(end, atomicProgressSize.get()));
                            long size = atomicProgressSize.get();
                            int processorCount = RuntimeUtil.getProcessorCount();
                            log.info("{}-{} {} {},读取文件文件 {} 内容花费：{}", processorCount, total, fileSize, FileUtil.readableFileSize(size), NumberUtil.formatPercent(((double) size / length), 0), DateUtil.formatBetween(SystemClock.now() - start));
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
        log.info("读取文件文件内容花费：" + DateUtil.formatBetween(SystemClock.now() - start));

    }
}
