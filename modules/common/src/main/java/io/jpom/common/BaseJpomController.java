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
package io.jpom.common;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.controller.base.AbstractController;

/**
 * controller
 *
 * @author jiangzeyin
 * @since 2019/4/16
 */
public abstract class BaseJpomController extends AbstractController {
    /**
     * 路径安全格式化
     *
     * @param path 路径
     * @return 去掉 提权字符串
     */
    public static String pathSafe(String path) {
        if (path == null) {
            return null;
        }
        String newPath = path.replace("../", StrUtil.EMPTY);
        newPath = newPath.replace("..\\", StrUtil.EMPTY);
        newPath = newPath.replace("+", StrUtil.EMPTY);
        return FileUtil.normalize(newPath);
    }

    protected boolean checkPathSafe(String path) {
        if (path == null) {
            return false;
        }
        String newPath = path.replace("../", StrUtil.EMPTY);
        newPath = newPath.replace("..\\", StrUtil.EMPTY);
        newPath = newPath.replace("+", StrUtil.EMPTY);
        return newPath.equals(path);
    }
}
