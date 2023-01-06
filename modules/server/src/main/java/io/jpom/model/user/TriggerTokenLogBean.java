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
package io.jpom.model.user;

import io.jpom.service.ITriggerToken;
import lombok.Data;
import lombok.EqualsAndHashCode;
import top.jpom.h2db.TableName;
import top.jpom.model.BaseDbModel;

/**
 * id 为 triggerToken
 *
 * @author bwcx_jzy
 * @since 2022/7/22
 */
@TableName(value = "TRIGGER_TOKEN_LOG", name = "触发器 token")
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
     * 用户ID
     *
     * @see UserModel#getId()
     */
    private String userId;
}
