/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.model.data;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.model.BaseJsonModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
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
        Assert.state(contains, I18nMessageUtil.get("i18n.no_current_static_directory_permission.ed70"));
    }

    /**
     * 判断指定 url 是否在授权范围
     *
     * @param url url 地址
     */
    public void checkAllowRemoteDownloadHost(String url) {
        Set<String> allowRemoteDownloadHost = this.getAllowRemoteDownloadHost();
        Assert.state(CollUtil.isNotEmpty(allowRemoteDownloadHost), I18nMessageUtil.get("i18n.remote_addresses_not_configured.275e"));
        List<String> collect = allowRemoteDownloadHost.stream()
            .filter(s -> StrUtil.startWith(url, s))
            .collect(Collectors.toList());
        Assert.state(CollUtil.isNotEmpty(collect), I18nMessageUtil.get("i18n.disallowed_download.06a3"));
    }
}
