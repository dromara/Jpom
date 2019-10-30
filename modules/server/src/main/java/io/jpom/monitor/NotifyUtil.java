package io.jpom.monitor;

import io.jpom.model.BaseEnum;
import io.jpom.model.data.MonitorModel;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通知util
 *
 * @author bwcx_jzy
 * @date 2019/7/13
 */
public class NotifyUtil {

    private static final Map<MonitorModel.NotifyType, INotify> NOTIFY_MAP = new ConcurrentHashMap<>();

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
    public static void send(MonitorModel.Notify notify, String title, String context) {
        int style = notify.getStyle();
        MonitorModel.NotifyType notifyType = BaseEnum.getEnum(MonitorModel.NotifyType.class, style);
        Objects.requireNonNull(notifyType);
        //
        INotify iNotify = NOTIFY_MAP.get(notifyType);
        Objects.requireNonNull(iNotify);
        iNotify.send(notify, title, context);
    }

}
