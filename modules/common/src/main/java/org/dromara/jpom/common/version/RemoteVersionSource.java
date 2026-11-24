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

/**
 * 远端版本信息来源（在线刷新 + 本机缓存）
 *
 * <p>变点：远端版本信息「从哪来」（官方下载中心 / 中转地址 / 本机缓存文件）。新增一种来源＝新增一个实现，
 * {@link RemoteVersionResolver} 与全部调用方零改动。</p>
 *
 * @author jpom
 * @since 2.11.14.7
 */
public interface RemoteVersionSource {

    /**
     * 在线刷新远端版本信息（一次尝试）
     *
     * @return 远端版本信息；网络异常或返回内容不可用时返回 null
     */
    RemoteVersion fetch();

    /**
     * 读取本机缓存的远端版本信息（缓存过期由实现方判断）
     *
     * @return 缓存的远端版本信息；不存在或已过期返回 null
     */
    RemoteVersion cached();
}
