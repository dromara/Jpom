package cn.keepbx.monitor;

import cn.keepbx.jpom.model.BaseEnum;
import cn.keepbx.jpom.model.data.MonitorModel;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author bwcx_jzy
 * @date 2019/7/13
 */
public class NotifyUtil {

    private static final Map<MonitorModel.NotifyType, INotify> NOTIFY_MAP = new ConcurrentHashMap<>();

    static {
        NOTIFY_MAP.put(MonitorModel.NotifyType.dingding, new DingTalkUtil());
        NOTIFY_MAP.put(MonitorModel.NotifyType.mail, new EmailUtil());
    }

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
