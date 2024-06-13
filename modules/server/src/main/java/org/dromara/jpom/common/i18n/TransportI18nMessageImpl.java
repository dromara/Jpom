package org.dromara.jpom.common.i18n;

import org.dromara.jpom.transport.i18n.II18nMessageUtil;

/**
 * @author bwcx_jzy1
 * @since 2024/6/11
 */
public class TransportI18nMessageImpl implements II18nMessageUtil {

    @Override
    public String get(String key) {
        return I18nMessageUtil.get(key);
    }
}
