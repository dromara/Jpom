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
package io.jpom;

import cn.hutool.core.date.BetweenFormatter;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.SystemClock;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.jiangzeyin.common.EnableCommonBoot;
import io.jpom.common.JpomAppType;
import io.jpom.common.Type;
import io.jpom.common.interceptor.IpInterceptor;
import io.jpom.common.interceptor.LoginInterceptor;
import io.jpom.common.interceptor.OpenApiInterceptor;
import io.jpom.common.interceptor.PermissionInterceptor;
import io.jpom.model.data.SystemIpConfigModel;
import io.jpom.service.system.SystemParametersServer;
import io.jpom.service.user.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

/**
 * jpom 启动类
 *
 * @author jiangzeyin
 * @since 2017/9/14
 */
@SpringBootApplication
@ServletComponentScan
@EnableCommonBoot
@Slf4j
@JpomAppType(Type.Server)
public class JpomServerApplication {

    /**
     * 启动执行
     * <p>
     * --rest:ip_config 重置 IP 白名单配置
     * <p>
     * --rest:load_init_db 重新加载数据库初始化操作
     * <p>
     * --rest:super_user_pwd 重置超级管理员密码
     * <p>
     * --recover:h2db 当 h2 数据出现奔溃无法启动需要执行恢复逻辑
     * <p>
     * --close:super_user_mfa 关闭超级管理员 mfa
     * <p>
     * --backup-h2 备份数据库
     * <p>
     * --import-h2-sql=/xxxx.sql 导入指定文件 sql
     * <p>
     * --replace-import-h2-sql=/xxxx.sql 替换导入指定文件 sql（会删除掉已经存的数据）
     * <p>
     * --transform-sql 转换 sql 内容(低版本兼容高版本),仅在导入 sql 文件时候生效：--import-h2-sql=/xxxx.sql、--replace-import-h2-sql=/xxxx.sql
     *
     * @param args 参数
     * @throws Exception 异常
     */
    public static void main(String[] args) throws Exception {
        long time = SystemClock.now();
        //
        JpomApplication jpomApplication = new JpomApplication(JpomServerApplication.class);
        jpomApplication
            // 拦截器
            .addInterceptor(IpInterceptor.class)
            .addInterceptor(LoginInterceptor.class)
            .addInterceptor(OpenApiInterceptor.class)
            .addInterceptor(PermissionInterceptor.class)
            .run(args);
        // 重置 ip 白名单配置
        if (ArrayUtil.containsIgnoreCase(args, "--rest:ip_config")) {
            SystemParametersServer parametersServer = SpringUtil.getBean(SystemParametersServer.class);
            parametersServer.delByKey(SystemIpConfigModel.ID);
            log.info("Clear IP whitelist configuration successfully");
        }
        //  重置超级管理员密码
        if (ArrayUtil.containsIgnoreCase(args, "--rest:super_user_pwd")) {
            UserService userService = SpringUtil.getBean(UserService.class);
            String restResult = userService.restSuperUserPwd();
            if (restResult != null) {
                log.info(restResult);
            } else {
                log.error("There is no super administrator account in the system");
            }
        }
        // 关闭超级管理员 mfa
        if (ArrayUtil.containsIgnoreCase(args, "--close:super_user_mfa")) {
            UserService userService = SpringUtil.getBean(UserService.class);
            String restResult = userService.closeSuperUserMfa();
            if (restResult != null) {
                log.info(restResult);
            } else {
                log.error("There is no super administrator account in the system");
            }
        }


        log.info("Time-consuming to start this time：{}", DateUtil.formatBetween(SystemClock.now() - time, BetweenFormatter.Level.MILLISECOND));
    }
}
