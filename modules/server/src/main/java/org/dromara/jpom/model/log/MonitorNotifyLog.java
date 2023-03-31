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

import cn.hutool.core.util.ObjectUtil;
import org.dromara.jpom.model.BaseWorkspaceModel;
import org.dromara.jpom.model.data.MonitorModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.jpom.db.TableName;

/**
 * 监控日志
 *
 * @author bwcx_jzy
 * @since 2019/7/13
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "MONITORNOTIFYLOG", name = "监控通知")
@Data
public class MonitorNotifyLog extends BaseWorkspaceModel {


    private String nodeId;
    private String projectId;
    /**
     * 异常发生时间
     */
    private Long createTime;
    private String title;
    private String content;
    /**
     * 项目状态状态
     */
    private Boolean status;
    /**
     * 通知方式
     *
     * @see MonitorModel.NotifyType
     */
    private Integer notifyStyle;
    /**
     * 通知发送状态
     */
    private Boolean notifyStatus;
    /**
     * 监控id
     */
    private String monitorId;
    /**
     * 通知对象
     */
    private String notifyObject;
    /**
     * 通知异常消息
     */
    private String notifyError;

    public boolean status() {
        return ObjectUtil.defaultIfNull(status, false);
    }
}
