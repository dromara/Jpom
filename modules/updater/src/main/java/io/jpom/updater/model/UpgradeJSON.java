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
package io.jpom.updater.model;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * upgrade.json 数据文件模型
 *
 * @author hjk
 * @date 12/19/2022
 */
@Accessors(chain = true)
@Data
public class UpgradeJSON {

    private String newJar;

    private Integer upgradeCount;

    private String beforeJar;

    private String oldLogName;

    private String updateTime;

    private Boolean logBack;

    public boolean isAgentJar() {
        return StrUtil.containsIgnoreCase(this.newJar, "agent");
    }

    public boolean isServerJar() {
        return StrUtil.containsIgnoreCase(this.newJar, "server");
    }

    public String getVersion() {
        // Server-2.8.16.jar
        String[] split = this.newJar.split("-");

        // 0 - Server
        // 1 - 2.8.16.jar
        Assert.isTrue(split.length == 2, "包不正确");

        // 0 - 2
        // 1 - 8
        // 2 - 16
        // 3 - jar
        String[] split1 = split[1].split("\\.");
        return String.join(".", split1[0], split1[1], split1[2]);
    }
}
