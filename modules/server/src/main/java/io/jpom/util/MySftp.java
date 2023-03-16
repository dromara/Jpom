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

import cn.hutool.core.io.FileUtil;
import cn.hutool.extra.ssh.Sftp;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpProgressMonitor;

import java.io.File;
import java.nio.charset.Charset;

/**
 * @author bwcx_jzy
 * @since 2023/3/16
 */
public class MySftp extends Sftp {

    private final ProgressMonitor progressMonitor;

    public interface ProgressMonitor {

        /**
         * 进度回调
         *
         * @param desc 远程目录
         * @param max  文件总大小
         * @param now  当前进程
         */
        void progress(String desc, long max, long now);

        /**
         * 重置
         */
        void rest();
    }

    public MySftp(Session session, Charset charset, long timeOut, ProgressMonitor monitor) {
        super(session, charset, timeOut);
        this.progressMonitor = monitor;
    }

    @Override
    public boolean upload(String destPath, File file) {
        final String[] desc = new String[1];
        final long[] maxLen = new long[1];
        progressMonitor.rest();
        SftpProgressMonitor sftpProgressMonitor = new SftpProgressMonitor() {
            private long totalCount = 0;

            @Override
            public void init(int op, String src, String dest, long max) {
                desc[0] = dest;
                maxLen[0] = max;
            }

            @Override
            public boolean count(long count) {
                totalCount += count;
                progressMonitor.progress(desc[0], maxLen[0], totalCount);
                return true;
            }

            @Override
            public void end() {

            }
        };
        super.put(FileUtil.getAbsolutePath(file), destPath, sftpProgressMonitor, Mode.OVERWRITE);
        return true;
    }
}
