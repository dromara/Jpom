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
        boolean allow = allowRemoteDownloadHost.stream().anyMatch(s -> matchRemoteDownloadHost(url, s));
        Assert.state(allow, I18nMessageUtil.get("i18n.disallowed_download.06a3"));
    }

    /**
     * 判断下载地址是否命中一条白名单。
     * <p>
     * 只做字符串前缀匹配会被绕过：白名单 {@code http://allowed.example} 下，
     * {@code http://allowed.example.evil/}（子域拼接）与 {@code http://allowed.example@evil/}（userinfo）
     * 都能通过前缀校验却请求到攻击者主机（SSRF）。因此命中后还要求主机部分之后紧跟路径/查询分隔符或结尾。
     *
     * @param url   下载地址
     * @param allow 白名单条目
     * @return true 表示允许
     */
    static boolean matchRemoteDownloadHost(String url, String allow) {
        String target = StrUtil.trimToEmpty(url);
        String base = StrUtil.trimToEmpty(allow);
        if (StrUtil.isEmpty(target) || StrUtil.isEmpty(base)) {
            return false;
        }
        if (!StrUtil.startWithIgnoreCase(target, base)) {
            return false;
        }
        if (StrUtil.endWith(base, StrUtil.SLASH)) {
            // 白名单已写到路径边界，前缀命中即可
            return true;
        }
        String rest = StrUtil.subSuf(target, base.length());
        if (StrUtil.isEmpty(rest)) {
            return true;
        }
        // 主机之后必须是路径/查询/锚点分隔符，才算同一个 host
        char first = rest.charAt(0);
        return first == '/' || first == '?' || first == '#';
    }
}
