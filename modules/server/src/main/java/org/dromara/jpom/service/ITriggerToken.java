/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service;

import cn.keepbx.jpom.model.BaseIdModel;

/**
 * 带有触发器 token 相关实现服务
 *
 * @author bwcx_jzy
 * @since 2022/7/22
 */
public interface ITriggerToken {

    /**
     * 类型 名称
     *
     * @return 数据分类名称
     */
    String typeName();

    /**
     * 数据描述
     *
     * @return 描述
     */
    String getDataDesc();

    /**
     * 判断是否存在
     *
     * @param dataId 数据id
     * @return true 存在
     */
    boolean exists(String dataId);

    BaseIdModel getByKey(String keyValue);

    /**
     * 写回触发器 token
     * <p>
     * 供统一收口点 {@code TriggerUrlAssembler} 在生成/重置 token 后落库，避免每个入口自己拼更新实体。
     * 默认抛异常只是为了让漏实现的类型在开发期立刻暴露，生产代码里所有实现都必须覆盖。
     * </p>
     *
     * @param id           数据 ID
     * @param triggerToken 新 token
     */
    default void updateTriggerToken(String id, String triggerToken) {
        throw new UnsupportedOperationException("updateTriggerToken not implemented: " + this.typeName());
    }
}
