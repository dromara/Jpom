/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom;

import cn.hutool.core.date.BetweenFormatter;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.SystemClock;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.keepbx.jpom.JpomAppType;
import cn.keepbx.jpom.Type;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.ServerOpenApi;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.system.AgentStartInit;
import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.ServletComponentScan;

/**
 * jpom 启动类
 *
 * @author bwcx_jzy
 * @since 2017/9/14.
 */
@SpringBootApplication(scanBasePackages = {"org.dromara.jpom"})
@ServletComponentScan(basePackages = {"org.dromara.jpom"})
@Slf4j
@JpomAppType(Type.Agent)
public class JpomAgentApplication {

    /**
     * 启动执行
     *
     * @param args 参数
     * @throws Exception 异常
     */
    public static void main(String[] args) throws Exception {
        long time = SystemClock.now();
        SpringApplicationBuilder springApplicationBuilder = new SpringApplicationBuilder(JpomAgentApplication.class);
        springApplicationBuilder.bannerMode(Banner.Mode.LOG);
        springApplicationBuilder.run(args);
        // 自动向服务端推送
        autoPushToServer(args);
        log.info("Time-consuming to start this time：{}", DateUtil.formatBetween(SystemClock.now() - time, BetweenFormatter.Level.MILLISECOND));
    }

    /**
     * 自动推送 插件端信息到服务端
     *
     * @param args 参数
     */
    private static void autoPushToServer(String[] args) {
        int i = ArrayUtil.indexOf(args, ServerOpenApi.PUSH_NODE_KEY);
        if (i == ArrayUtil.INDEX_NOT_FOUND) {
            return;
        }
        String arg = ArrayUtil.get(args, i + 1);
        if (StrUtil.isEmpty(arg)) {
            log.error("not found auto-push-to-server url");
            return;
        }
        try {
            AgentStartInit autoRegSeverNode = SpringUtil.getBean(AgentStartInit.class);
            autoRegSeverNode.autoPushToServer(arg);
        } catch (Exception e) {
            log.error(I18nMessageUtil.get("i18n.push_registration_to_server_failed.5949"), arg, e);
        }
    }

}
