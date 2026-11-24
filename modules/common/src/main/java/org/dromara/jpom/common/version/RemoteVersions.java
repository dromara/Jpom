/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.common.version;

import cn.keepbx.jpom.RemoteVersion;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.i18n.I18nMessageUtil;

/**
 * 远端版本信息对外门面
 *
 * <p>调用方只认这三种语义：页面展示、显式检查更新、升级/下载动作；重试与缓存兜底在
 * {@link RemoteVersionResolver} 内完成，用户可见文案统一由本类出 i18n。</p>
 *
 * @author jpom
 * @since 2.11.14.7
 */
@Slf4j
public final class RemoteVersions {

    private RemoteVersions() {
    }

    /**
     * 页面展示用（缓存优先，缓存缺失时在线刷新一次）
     *
     * @return 远端版本信息；拿不到返回 null
     */
    public static RemoteVersion infoForDisplay() {
        return RemoteVersionResolver.defaultResolver().resolveForDisplay();
    }

    /**
     * 显式「检查更新」用（刷新失败自动重试，仍失败回退缓存）
     *
     * @return 远端版本信息；拿不到返回 null
     */
    public static RemoteVersion infoForRefresh() {
        RemoteVersion remoteVersion = RemoteVersionResolver.defaultResolver().resolveForRefresh();
        if (remoteVersion == null) {
            log.warn(I18nMessageUtil.get("i18n.remote_version_unavailable.b7e1"));
        }
        return remoteVersion;
    }

    /**
     * 升级/下载动作用（刷新失败自动重试，仍失败回退缓存，全部失败抛可读异常）
     *
     * @return 远端版本信息
     * @throws IllegalStateException 在线刷新与本机缓存都拿不到远端版本信息（文案已 i18n）
     */
    public static RemoteVersion infoForUpgrade() {
        try {
            return RemoteVersionResolver.defaultResolver().resolveForAction();
        } catch (RemoteVersionResolver.UnavailableException e) {
            log.warn("remote version info unavailable: online refresh and local cache are both empty");
            String msg = I18nMessageUtil.get("i18n.remote_version_unavailable.b7e1");
            throw new IllegalStateException(msg);
        }
    }
}
