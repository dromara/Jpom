package org.dromara.jpom.oauth2.platform;

import cn.hutool.core.lang.RegexPool;
import cn.hutool.core.lang.Validator;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.oauth2.BaseOauth2Config;

/**
 * @author bwcx_jzy
 * @since 2024/12/3
 */
@EqualsAndHashCode(callSuper = true)
@Data
public abstract class CustomOauth2Config extends BaseOauth2Config {

    private String authorizationUri;
    private String accessTokenUri;
    private String userInfoUri;


    /**
     * 验证数据
     */
    public void check() {
        super.check();
        Validator.validateMatchRegex(RegexPool.URL_HTTP, this.authorizationUri, I18nMessageUtil.get("i18n.configure_correct_auth_url.22e7"));
        Validator.validateMatchRegex(RegexPool.URL_HTTP, this.accessTokenUri, I18nMessageUtil.get("i18n.configure_correct_token_url.7bba"));
        Validator.validateMatchRegex(RegexPool.URL_HTTP, this.userInfoUri, I18nMessageUtil.get("i18n.configure_correct_user_info_url.1276"));
    }
}
