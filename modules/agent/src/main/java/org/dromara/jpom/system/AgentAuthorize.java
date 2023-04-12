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
package org.dromara.jpom.system;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.alibaba.fastjson2.JSONObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.common.Const;
import org.dromara.jpom.common.ILoadEvent;
import org.dromara.jpom.model.system.AgentAutoUser;
import org.dromara.jpom.util.JsonFileUtil;
import org.dromara.jpom.util.JvmUtil;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

import java.io.File;

/**
 * agent 端授权账号信息
 *
 * @author bwcx_jzy
 * @since 2019/4/17
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties("jpom.authorize")
public class AgentAuthorize implements ILoadEvent {
    /**
     * 账号
     */
    private String agentName;
    /**
     * 密码
     */
    private String agentPwd;
    /**
     * 授权加密字符串
     */
    private String authorize;

    public void setAuthorize(String authorize) {
        // 不能外部 set
    }

    public String getAuthorize() {
        return null;
    }

    private final JpomApplication configBean;
    /**
     * 注入控制加载顺序，必须先加载数据目录才能初始化
     */
    private final AgentConfig agentConfig;

    public AgentAuthorize(JpomApplication configBean,
                          AgentConfig agentConfig) {
        this.configBean = configBean;
        this.agentConfig = agentConfig;
    }

    /**
     * 判断授权是否正确
     *
     * @param authorize 授权
     * @return true 正确
     */
    public boolean checkAuthorize(String authorize) {
        return StrUtil.equals(authorize, this.authorize);
    }

    /**
     * 检查是否配置密码
     */
    private void checkPwd() {
        File path = FileUtil.file(configBean.getDataPath(), Const.AUTHORIZE);
        if (StrUtil.isNotEmpty(agentPwd)) {
            // 有指定密码 清除旧密码信息
            FileUtil.del(path);
            log.info("Authorization information has been customized,account：{}", this.agentName);
            return;
        }
        if (FileUtil.exist(path)) {
            // 读取旧密码
            String json = FileUtil.readString(path, CharsetUtil.CHARSET_UTF_8);
            AgentAutoUser autoUser = JSONObject.parseObject(json, AgentAutoUser.class);
            if (!StrUtil.equals(autoUser.getAgentName(), this.agentName)) {
                throw new JpomRuntimeException("The existing login name is inconsistent with the configured login name");
            }
            String oldAgentPwd = autoUser.getAgentPwd();
            if (StrUtil.isNotEmpty(oldAgentPwd)) {
                this.agentPwd = oldAgentPwd;
                log.info("Already authorized account:{} password:{} Authorization information storage location：{}", this.agentName, this.agentPwd, FileUtil.getAbsolutePath(path));
                return;
            }
        }
        this.agentPwd = RandomUtil.randomString(10);
        AgentAutoUser autoUser = new AgentAutoUser();
        autoUser.setAgentName(this.agentName);
        autoUser.setAgentPwd(this.agentPwd);
        // 写入文件中
        JsonFileUtil.saveJson(path, autoUser.toJson());
        log.info("Automatically generate authorized account:{}  password:{}  Authorization information storage location：{}", this.agentName, this.agentPwd, FileUtil.getAbsolutePath(path));
    }

    @Override
    public void afterPropertiesSet(ApplicationContext applicationContext) throws Exception {
        // 登录名不能为空
        if (StrUtil.isEmpty(this.agentName)) {
            throw new JpomRuntimeException("The agent login name cannot be empty");
        }
        if (StrUtil.isEmpty(this.authorize)) {
            this.checkPwd();
            // 生成密码授权字符串
            this.authorize = SecureUtil.sha1(this.agentName + "@" + this.agentPwd);
        } else {
            log.warn("authorized 不能重复加载");
        }
        //
        JvmUtil.checkJpsNormal();
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }
}
