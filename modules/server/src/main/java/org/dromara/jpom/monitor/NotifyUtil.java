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

import cn.hutool.core.map.SafeConcurrentHashMap;
import org.dromara.jpom.model.BaseEnum;
import org.dromara.jpom.model.data.MonitorModel;

import java.util.Map;
import java.util.Objects;

/**
 * 通知util
 *
 * @author bwcx_jzy
 * @since 2019/7/13
 */
public class NotifyUtil {

	private static final Map<MonitorModel.NotifyType, INotify> NOTIFY_MAP = new SafeConcurrentHashMap<>();

	static {
		NOTIFY_MAP.put(MonitorModel.NotifyType.dingding, new WebHookUtil());
		NOTIFY_MAP.put(MonitorModel.NotifyType.mail, new EmailUtil());
		NOTIFY_MAP.put(MonitorModel.NotifyType.workWx, new WebHookUtil());
	}

	/**
	 * 发送报警消息
	 *
	 * @param notify  通知方式
	 * @param title   描述
	 * @param context 内容
	 */
	public static void send(MonitorModel.Notify notify, String title, String context) throws Exception {
		int style = notify.getStyle();
		MonitorModel.NotifyType notifyType = BaseEnum.getEnum(MonitorModel.NotifyType.class, style);
		Objects.requireNonNull(notifyType);
		//
		INotify iNotify = NOTIFY_MAP.get(notifyType);
		Objects.requireNonNull(iNotify);
		iNotify.send(notify, title, context);
	}

}
