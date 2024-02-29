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
