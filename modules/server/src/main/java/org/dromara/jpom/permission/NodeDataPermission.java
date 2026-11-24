/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.permission;

import org.dromara.jpom.service.h2db.BaseNodeService;

import java.lang.annotation.*;

/**
 * @author bwcx_jzy
 * @since 2021/12/23
 */

@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface NodeDataPermission {

    /**
     * 参数名
     *
     * @return 默认ID
     */
    String parameterName() default "id";

    /**
     * 允许「数据尚未入库」的新增语义。
     * <p>
     * 默认 false：节点数据缓存未命中时按无权限拒绝（fail-closed，2026-11-08 审计 P3-4）。
     * 但「新增」场景下数据本来就还不存在——例如新增项目时前端必填项目 ID，服务端缓存里必然查不到，
     * 于是非超级管理员会被误判成越权（2.11.13.1 起的群反馈缺陷）。
     * 这类方法显式声明 allowMissingData = true 后，数据不存在时放行到方法内部继续校验：
     * 工作空间权限仍由方法级 {@code @Feature} 把关，数据归属仍由方法内部校验
     * （如项目保存的 checkProjectPermission 会拒绝「id 属于其它工作空间」的情况）。
     *
     * @return true 表示数据不存在时放行到方法内部继续校验
     */
    boolean allowMissingData() default false;

    /**
     * 数据 class
     *
     * @return cls
     */
    Class<? extends BaseNodeService<?>> cls();
}
