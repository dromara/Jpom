/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.configuration;

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
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.model.system.AgentAutoUser;
import org.dromara.jpom.system.JpomRuntimeException;
import org.dromara.jpom.util.JsonFileUtil;
import org.dromara.jpom.util.JvmUtil;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

/**
 * agent 端授权账号信息
 *
 * @author bwcx_jzy
 * @since 2019/4/17
 */
@Slf4j
@Data
@ConfigurationProperties("jpom.authorize")
public class AgentAuthorize {
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
    private void checkPwd(JpomApplication configBean) {
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
                restrictFilePermission(path);
                log.info("Already authorized account:{} Authorization information storage location：{}", this.agentName, FileUtil.getAbsolutePath(path));
                return;
            }
        }
        this.agentPwd = RandomUtil.randomString(10);
        AgentAutoUser autoUser = new AgentAutoUser();
        autoUser.setAgentName(this.agentName);
        autoUser.setAgentPwd(this.agentPwd);
        // 写入文件中
        JsonFileUtil.saveJson(path, autoUser.toJson());
        restrictFilePermission(path);
        log.info("Automatically generate authorized account:{}  Authorization information storage location：{}", this.agentName, FileUtil.getAbsolutePath(path));
    }

    /**
     * 收紧授权信息文件权限，避免同机其他用户直接读取明文密码
     * <p>
     * 非 POSIX 文件系统（如 Windows）不支持时忽略
     *
     * @param file 授权信息文件
     */
    private static void restrictFilePermission(File file) {
        if (file == null || !file.isFile()) {
            return;
        }
        Path path = file.toPath();
        try {
            if (Files.getFileAttributeView(path, PosixFileAttributeView.class) == null) {
                return;
            }
            Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rw-------");
            Files.setPosixFilePermissions(path, permissions);
        } catch (Exception e) {
            log.warn("Failed to restrict authorization information file permission: {}", FileUtil.getAbsolutePath(file), e);
        }
    }

    public void init(JpomApplication configBean) {
        if (StrUtil.isEmpty(this.agentName)) {
            throw new JpomRuntimeException("The agent login name cannot be empty");
        }
        if (StrUtil.isEmpty(this.authorize)) {
            this.checkPwd(configBean);
            // 生成密码授权字符串
            this.authorize = SecureUtil.sha1(this.agentName + "@" + this.agentPwd);
        } else {
            log.warn(I18nMessageUtil.get("i18n.authorized_cannot_be_reloaded.6ece"));
        }
        //
        JvmUtil.checkJpsNormal();
    }
}
