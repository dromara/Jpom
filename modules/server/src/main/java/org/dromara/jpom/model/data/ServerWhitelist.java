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
package org.dromara.jpom.model.data;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.model.BaseJsonModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.util.Assert;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 节点分发授权
 *
 * @author bwcx_jzy
 * @since 2019/4/22
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ServerWhitelist extends BaseJsonModel {

    public static final String ID = "OUTGIVING_WHITELIST";

    /**
     * 不同工作空间的 ID
     *
     * @param workspaceId 工作空间ID
     * @return id
     */
    public static String workspaceId(String workspaceId) {
        return ServerWhitelist.ID + StrUtil.DASHED + workspaceId;
    }

    /**
     * 项目的授权
     */
    private List<String> outGiving;

    /**
     * 允许远程下载的 host
     */
    private Set<String> allowRemoteDownloadHost;

    /**
     * 静态目录
     */
    private List<String> staticDir;

    /**
     * 规范化路径
     *
     * @return list
     */
    public List<String> staticDir() {
        if (staticDir == null) {
            return new ArrayList<>();
        }
        return staticDir.stream()
            .map(s -> {
                // 规范化
                File file = FileUtil.file(s);
                String absolutePath = file.getAbsolutePath();
                return FileUtil.normalize(absolutePath);
            })
            .collect(Collectors.toList());
    }

    /**
     * 验证静态目录权限
     */
    public void checkStaticDir(String path) {
        List<String> dir = this.staticDir;
        boolean contains = CollUtil.contains(dir, path);
        Assert.state(contains, "没有当前静态目录权限");
    }

    public List<String> outGiving() {
        return AgentWhitelist.useConvert(outGiving);
    }

    /**
     * 判断指定 url 是否在授权范围
     *
     * @param url url 地址
     */
    public void checkAllowRemoteDownloadHost(String url) {
        Set<String> allowRemoteDownloadHost = this.getAllowRemoteDownloadHost();
        Assert.state(CollUtil.isNotEmpty(allowRemoteDownloadHost), "还没有配置允许的远程地址");
        List<String> collect = allowRemoteDownloadHost.stream()
            .filter(s -> StrUtil.startWith(url, s))
            .collect(Collectors.toList());
        Assert.state(CollUtil.isNotEmpty(collect), "不允许下载当前地址的文件");
    }
}
