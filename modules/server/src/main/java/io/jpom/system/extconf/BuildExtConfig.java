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
package io.jpom.system.extconf;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author bwcx_jzy
 * @since 2022/7/7
 */
@Configuration
@ConfigurationProperties(prefix = "build")
@Data
public class BuildExtConfig {

    /**
     * 构建最多保存多少份历史记录
     */
    private int maxHistoryCount = 1000;

    /**
     * 每一项构建最多保存的历史份数
     */
    private int itemMaxHistoryCount = 50;

    private Boolean checkDeleteCommand;

    /**
     * 构建线程池大小,小于 1 则为不限制，默认大小为 5
     */
    private int poolSize = 5;

    /**
     * 构建任务等待数量，超过此数量将取消构建任务，值最小为 1
     */
    private int poolWaitQueue = 10;


    public boolean checkDeleteCommand() {
        return checkDeleteCommand != null && checkDeleteCommand;
    }

}
