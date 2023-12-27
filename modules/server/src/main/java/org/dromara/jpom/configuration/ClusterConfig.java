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
package org.dromara.jpom.configuration;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import org.dromara.jpom.common.Const;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author bwcx_jzy
 * @since 23/12/25 025
 */
@Data
@ConfigurationProperties("jpom.cluster")
public class ClusterConfig {

    /**
     * 集群Id，默认为 default 不区分大小写，只能是字母或者数字，长度小于 20
     */
    private String id;
    /**
     * 检查节点心跳间隔时间,最小值 5 秒
     */
    private int heartSecond = 30;

    public int getHeartSecond() {
        return Math.max(this.heartSecond, 5);
    }

    public String getId() {
        return StrUtil.emptyToDefault(this.id, Const.WORKSPACE_DEFAULT_ID).toUpperCase();
    }
}
