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

/**
 * @author bwcx_jzy
 * @since 2023/2/11
 */
public interface ILogRecorder {

    /**
     * 记录单行日志
     *
     * @param info 日志
     * @param vals 变量参数
     * @return 格式化后的字符串
     */
    String info(String info, Object... vals);

    /**
     * 记录单行日志
     *
     * @param info 日志
     * @param vals 变量参数
     * @return 格式化后的字符串
     */
    String system(String info, Object... vals);

    /**
     * 记录单行日志
     *
     * @param info 日志
     * @param vals 变量参数
     * @return 格式化后的字符串
     */
    String systemError(String info, Object... vals);

    /**
     * 记录单行日志
     *
     * @param info 日志
     * @param vals 变量参数
     * @return 格式化后的字符串
     */
    String systemWarning(String info, Object... vals);
}
