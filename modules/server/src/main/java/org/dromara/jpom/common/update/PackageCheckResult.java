/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.common.update;

/**
 * 升级包与节点兼容性检查结果
 *
 * @author jpom
 * @since 2.11.13
 */
public enum PackageCheckResult {
    /**
     * 通过
     */
    OK,
    /**
     * 升级包不合法（无版本号等）
     */
    INVALID_PACKAGE,
    /**
     * 节点引擎与升级包引擎不匹配
     */
    ENGINE_MISMATCH,
    /**
     * 节点平台与升级包平台不匹配（含 legacy 包/旧节点只能使用 linux/amd64 的限制）
     */
    PLATFORM_MISMATCH,
    /**
     * 新包版本和正在运行的版本一致
     */
    SAME_VERSION,
    /**
     * 当前版本低于升级包要求的最低兼容版本
     */
    MIN_VERSION,
    /**
     * 不允许降级
     */
    DOWNGRADE
}
