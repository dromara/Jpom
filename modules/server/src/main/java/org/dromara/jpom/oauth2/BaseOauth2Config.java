/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.oauth2;

import cn.hutool.core.lang.RegexPool;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ReflectUtil;
import lombok.Data;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.request.AuthRequest;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.springframework.util.Assert;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * @author bwcx_jzy
 * @since 2023/3/30
 */
@Data
public abstract class BaseOauth2Config {

    public static final Map<String, Tuple> DB_KEYS = new HashMap<>();

    static {
        Set<Class<?>> classes = ClassUtil.scanPackageBySuper(BaseOauth2Config.class.getPackage().getName(), BaseOauth2Config.class);
        for (Class<?> aClass : classes) {
            Field field = ReflectUtil.getField(aClass, "KEY");
            Assert.notNull(field, I18nMessageUtil.get("i18n.key_field_not_configured.7b22") + aClass.getName());
            String staticFieldValue = (String) ReflectUtil.getStaticFieldValue(field);
            BaseOauth2Config baseOauth2Config = (BaseOauth2Config) ReflectUtil.newInstanceIfPossible(aClass);
            DB_KEYS.put(baseOauth2Config.provide(), new Tuple(staticFieldValue, aClass));
        }
    }

    /**
     * 数据库存储的 key
     *
     * @param provide 平台名
     * @return 配置对象
     */
    public static Tuple getDbKey(String provide) {
        return DB_KEYS.get(provide);
    }

    protected Boolean enabled;
    protected String clientId;
    protected String clientSecret;
    protected String redirectUri;
    /**
     * 是否自动创建用户
     */
    protected Boolean autoCreteUser;
    protected Boolean ignoreCheckState;
    /**
     * 创建用户后，自动关联权限组
     */
    protected String permissionGroup;


    /**
     * 是否开启
     *
     * @return true 开启
     */
    public boolean enabled() {
        return enabled != null && enabled;
    }

    /**
     * 是否自动创建用户
     *
     * @return true 开启
     */
    public boolean autoCreteUser() {
        return autoCreteUser != null && autoCreteUser;
    }


    /**
     * 验证数据
     */
    public void check() {
        Assert.hasText(this.clientId, I18nMessageUtil.get("i18n.client_id_not_configured.ab8e"));
        Assert.hasText(this.clientSecret, I18nMessageUtil.get("i18n.client_secret_not_configured.6923"));
        Validator.validateMatchRegex(RegexPool.URL_HTTP, this.redirectUri, I18nMessageUtil.get("i18n.configure_correct_redirect_url.058e"));
    }

    /**
     * 供应商
     *
     * @return 返回供应商
     */
    public abstract String provide();

    /**
     * oauth2 请求对象
     *
     * @return AuthRequest
     */
    public abstract AuthRequest authRequest();

    public <T> T getValue(Function<BaseOauth2Config, T> function, T defaultValue) {
        return Optional.ofNullable(function.apply(this)).orElse(defaultValue);
    }

    public AuthConfig authConfig() {
        return AuthConfig.builder()
            .clientId(this.clientId)
            .clientSecret(this.clientSecret)
            .redirectUri(this.redirectUri)
            .ignoreCheckState(this.getValue(BaseOauth2Config::getIgnoreCheckState, false))
            .build();
    }
}
