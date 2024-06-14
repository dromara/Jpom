/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.model.user;

import cn.hutool.core.annotation.PropIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.jpom.db.TableName;
import org.dromara.jpom.model.BaseDbModel;
import org.dromara.jpom.service.ITriggerToken;

/**
 * id 为 triggerToken
 *
 * @author bwcx_jzy
 * @since 2022/7/22
 */
@TableName(value = "TRIGGER_TOKEN_LOG",
    nameKey = "i18n.trigger_token.abe6")
@Data
@EqualsAndHashCode(callSuper = true)
public class TriggerTokenLogBean extends BaseDbModel {
    /**
     * 为了兼容旧数据（因为旧数据字段长度大于 50 ）
     * <p>
     * 198fc5b944a3978b839506eb9d534c6f2b200dcda4e1d378fe30d2e8dbd7335bf4a
     */
    private String triggerToken;
    /**
     * 类型
     *
     * @see ITriggerToken#typeName()
     */
    private String type;

    /**
     * 关联数据ID
     */
    private String dataId;
    /**
     * 关联数据名称
     */
    @PropIgnore
    private String dataName;

    /**
     * 用户ID
     *
     * @see UserModel#getId()
     */
    private String userId;
    /**
     * 触发次数
     */
    private Integer triggerCount;
}
