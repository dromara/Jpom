package io.jpom.monitor;

import io.jpom.model.data.MonitorModel;

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
     * @param notify  通知方式
     * @param title   标题
     * @param context 内容
     */
    void send(MonitorModel.Notify notify, String title, String context);
}
