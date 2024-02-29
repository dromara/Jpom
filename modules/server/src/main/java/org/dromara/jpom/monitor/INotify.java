/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.monitor;

import org.dromara.jpom.model.data.MonitorModel;

/**
 * 通知接口
 *
 * @author bwcx_jzy
 * @since 2019/7/13
 */
public interface INotify {

    /**
     * 发送通知
     *
     * @param notify  通知方式
     * @param title   标题
     * @param context 内容
     */
    void send(MonitorModel.Notify notify, String title, String context) throws Exception;
}
