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
package org.dromara.jpom.model.log;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.jpom.db.TableName;
import org.dromara.jpom.model.BaseWorkspaceModel;

/**
 * 用户操作日志
 *
 * @author bwcx_jzy
 * @since 2019/4/19
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "USEROPERATELOGV1", name = "用户操作日志", workspaceBind = 2)
@Data
public class UserOperateLogV1 extends BaseWorkspaceModel {
    /**
     * 操作ip
     */
    private String ip;
    /**
     * 用户ip
     */
    private String userId;
    /**
     * 节点id
     */
    private String nodeId;
    /**
     * 操作时间
     */
    private Long optTime;
    /**
     * 操作状态,业务状态码
     */
    private Integer optStatus;
    /**
     * 完整消息
     */
    private String resultMsg;
    /**
     * 请求参数
     */
    private String reqData;
    /**
     * 数据id
     */
    private String dataId;
    /**
     * 数据名称
     */
    private String dataName;
    /**
     * 浏览器标识
     */
    private String userAgent;

    private String classFeature;
    private String methodFeature;
    /**
     * 工作空间名称
     */
    private String workspaceName;

    private String username;
}
