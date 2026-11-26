/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.pipeline.repo;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import org.dromara.jpom.model.data.RepositoryModel;

/**
 * git 地址处理：带凭据的执行地址 + 脱敏后的展示地址
 * <p>
 * 规则与系统 git 插件一致（http 协议把账号口令写进 URL、ssh 原样），但日志里一律用脱敏地址，
 * 避免构建日志把仓库口令带出去：日志、异常信息、界面展示一律只用脱敏地址。
 * </p>
 *
 * @author jpom
 * @since 2.11.15.3
 */
final class GitUrls {

    private GitUrls() {
    }

    /**
     * 执行用地址（http 协议拼上账号口令，ssh 原样返回）
     *
     * @param repository 仓库配置
     * @return url
     */
    static String covertUrl(RepositoryModel repository) {
        String url = StrUtil.nullToEmpty(repository.getGitUrl());
        Integer protocol = repository.getProtocol();
        if (protocol == null || protocol != 0) {
            // ssh 或未识别：原样使用（与系统 git 插件一致）
            return url;
        }
        String plain = stripUserInfo(url);
        String username = StrUtil.nullToEmpty(repository.getUserName());
        String password = StrUtil.nullToEmpty(repository.getPassword());
        if (StrUtil.isEmpty(username)) {
            return plain;
        }
        int index = plain.indexOf("://");
        if (index < 0) {
            return plain;
        }
        String prefix = plain.substring(0, index + 3);
        String rest = plain.substring(index + 3);
        String userInfo = URLUtil.encodeAll(username) + ":" + URLUtil.encodeAll(password);
        return prefix + userInfo + "@" + rest;
    }

    /**
     * 展示用地址（去掉 URL 里的账号口令）
     *
     * @param url 原始地址
     * @return 脱敏地址
     */
    static String sanitize(String url) {
        return stripUserInfo(StrUtil.nullToEmpty(url));
    }

    private static String stripUserInfo(String url) {
        int scheme = url.indexOf("://");
        if (scheme < 0) {
            return url;
        }
        String prefix = url.substring(0, scheme + 3);
        String rest = url.substring(scheme + 3);
        int at = rest.indexOf('@');
        int slash = rest.indexOf('/');
        if (at < 0 || (slash >= 0 && at > slash)) {
            return url;
        }
        return prefix + rest.substring(at + 1);
    }
}
