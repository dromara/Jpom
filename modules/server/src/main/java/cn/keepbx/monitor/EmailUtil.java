package cn.keepbx.monitor;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.mail.MailAccount;
import cn.hutool.extra.mail.MailUtil;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.jpom.model.data.MailAccountModel;
import cn.keepbx.jpom.model.data.MonitorModel;
import cn.keepbx.jpom.service.monitor.MonitorMailConfigService;

import java.util.Objects;

/**
 * 邮件工具
 *
 * @author Arno
 */
public class EmailUtil implements INotify {

    private MonitorMailConfigService monitorMailConfigService;

    @Override
    public void send(MonitorModel.Notify notify, String title, String context) {
        if (monitorMailConfigService == null) {
            monitorMailConfigService = SpringUtil.getBean(MonitorMailConfigService.class);
        }
        MailAccountModel config = monitorMailConfigService.getConfig();
        Objects.requireNonNull(config);
        MailAccount mailAccount = new MailAccount();
        mailAccount.setUser(config.getUser());
        mailAccount.setPass(config.getPass());
        mailAccount.setFrom(config.getFrom());
        mailAccount.setPort(config.getPort());
        mailAccount.setHost(config.getHost());
        //
        if (config.getSslEnable() != null && config.getSslEnable()) {
            mailAccount.setSslEnable(config.getSslEnable());
            if (config.getSocketFactoryPort() != null) {
                mailAccount.setSocketFactoryPort(config.getSocketFactoryPort());
            }
        }
        mailAccount.setAuth(true);
        MailUtil.send(mailAccount, CollUtil.newArrayList(StrUtil.split(notify.getValue(), StrUtil.COMMA)), title, context, false);
    }
}
