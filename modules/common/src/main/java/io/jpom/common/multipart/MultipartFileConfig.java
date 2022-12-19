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
package io.jpom.common.multipart;

import cn.hutool.core.util.StrUtil;
import cn.hutool.system.UserInfo;

/**
 * 上传文件保存路径
 *
 * @author jiangzeyin
 * @since 2017/10/25
 */
public class MultipartFileConfig {

    private static String fileTempPath;
    private static final UserInfo USER_INFO = new UserInfo();

    /**
     * 设置文件上传保存路径
     *
     * @param fileTempPath path
     */
    public static void setFileTempPath(String fileTempPath) {
        MultipartFileConfig.fileTempPath = fileTempPath;
    }

    public static String getFileTempPath() {
        if (StrUtil.isBlank(fileTempPath)) {
            fileTempPath = USER_INFO.getTempDir();
        }
        return fileTempPath;
    }
}
