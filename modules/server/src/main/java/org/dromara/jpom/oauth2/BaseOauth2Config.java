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
package org.dromara.jpom.oauth2;

import cn.hutool.core.lang.RegexPool;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ReflectUtil;
import lombok.Data;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.request.AuthRequest;
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
            Assert.notNull(field, "没有配置 KEY 字段," + aClass.getName());
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
        Assert.hasText(this.clientId, "没有配置 clientId");
        Assert.hasText(this.clientSecret, "没有配置 clientSecret");
        Validator.validateMatchRegex(RegexPool.URL_HTTP, this.redirectUri, "请配置正确的重定向 url");
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
