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
package org.dromara.jpom.plugin;

import cn.hutool.core.lang.Tuple;

/**
 * Git处理
 * <br>
 * Created By Hong on 2023/3/31
 *
 * @author Hong
 */
public interface GitProcess {

    /**
     * 分支和标签列表
     *
     * @return tuple
     * @throws Exception 异常
     */
    Tuple branchAndTagList() throws Exception;

    /**
     * 拉取指定分支
     *
     * @return 拉取结果
     * @throws Exception 异常
     */
    String[] pull() throws Exception;

    /**
     * 拉取指定标签
     *
     * @return 拉取结果
     * @throws Exception 异常
     */
    String[] pullByTag() throws Exception;

}
