package cn.keepbx.monitor;

import cn.keepbx.jpom.model.data.MonitorModel;

/**
 * 通知接口
 *
 * @author bwcx_jzy
 * @date 2019/7/13
 */
public interface INotify {

    /**
     * 发送通知
     *
     * @param notify
     * @param title
     * @param context
     */
    void send(MonitorModel.Notify notify, String title, String context);
}
