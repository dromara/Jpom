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
package org.dromara.jpom.common;

import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;

/**
 * jpom 加载事件
 * <p>
 * 保证在容器的 bean 加载完成之后
 *
 * @author bwcx_jzy
 * @since 2022/12/25
 */
public interface ILoadEvent extends Ordered {

    /**
     * 初始化成功后执行
     *
     * @param applicationContext 应用上下文
     * @throws Exception 异常
     */
    void afterPropertiesSet(ApplicationContext applicationContext) throws Exception;

    /**
     * 排序只
     *
     * @return 0 是默认
     */
    @Override
    default int getOrder() {
        return 0;
    }
}
