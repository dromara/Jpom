/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 码之科技工作室
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
package io.jpom.monitor;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.watch.SimpleWatcher;
import cn.hutool.core.io.watch.WatchMonitor;
import cn.hutool.core.io.watch.WatchUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.mail.MailAccount;
import cn.hutool.extra.mail.MailUtil;
import cn.jiangzeyin.common.spring.SpringUtil;
import io.jpom.model.data.MailAccountModel;
import io.jpom.model.data.MonitorModel;
import io.jpom.service.system.SystemMailConfigService;
import io.jpom.system.ConfigBean;
import io.jpom.system.ServerConfigBean;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.Objects;

/**
 * 邮件工具
 *
 * @author Arno
 */
public class EmailUtil implements INotify {

    private static SystemMailConfigService systemMailConfigService;
    private static MailAccountModel config;

    static {
        File file = FileUtil.file(ConfigBean.getInstance().getDataPath(), ServerConfigBean.MAIL_CONFIG);
        WatchMonitor monitor = WatchUtil.create(file, WatchMonitor.ENTRY_MODIFY);
        monitor.setWatcher(new SimpleWatcher() {
            @Override
            public void onModify(WatchEvent<?> event, Path currentPath) {
                // 读取新的内容
                config = systemMailConfigService.getConfig();
            }
        });
        monitor.start();
    }

    @Override
    public void send(MonitorModel.Notify notify, String title, String context) {
        MailAccount mailAccount = getAccount();
        MailUtil.send(mailAccount, StrUtil.split(notify.getValue(), StrUtil.COMMA), title, context, false);
    }

    public static MailAccount getAccountNew() {
        init();
        MailAccountModel config = systemMailConfigService.getConfig();
        return getAccount(config);
    }

    private static void init() {
        if (systemMailConfigService == null) {
            systemMailConfigService = SpringUtil.getBean(SystemMailConfigService.class);
        }
    }

    public static MailAccount getAccount() {
        if (config == null) {
            init();
            config = systemMailConfigService.getConfig();
        }
        return getAccount(config);
    }

    public static MailAccount getAccount(MailAccountModel config) {
        Objects.requireNonNull(config, "获取邮箱信息失败");
        MailAccount mailAccount = new MailAccount();
        mailAccount.setUser(config.getUser());
        mailAccount.setPass(config.getPass());
        mailAccount.setFrom(config.getFrom());
        mailAccount.setPort(config.getPort());
        mailAccount.setHost(config.getHost());
        //
        mailAccount.setTimeout(10 * 1000);
        mailAccount.setConnectionTimeout(10 * 1000);
        //
        if (config.getSslEnable() != null && config.getSslEnable()) {
            mailAccount.setSslEnable(config.getSslEnable());
            if (config.getSocketFactoryPort() != null) {
                mailAccount.setSocketFactoryPort(config.getSocketFactoryPort());
            }
        }
        mailAccount.setAuth(true);
        return mailAccount;
    }

    /**
     * 发送邮箱
     *
     * @param email   收件人
     * @param title   标题
     * @param context 内容
     */
    public static void send(String email, String title, String context) {
        MailAccount mailAccount = getAccount();
        MailUtil.send(mailAccount, email, title, context, false);
    }
}
