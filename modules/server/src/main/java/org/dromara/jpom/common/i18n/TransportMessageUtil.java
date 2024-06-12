package org.dromara.jpom.common.i18n;

import org.dromara.jpom.transport.i18n.IMessageUtil;

/**
 * @author bwcx_jzy1
 * @since 2024/6/11
 */
public class TransportMessageUtil implements IMessageUtil {

    @Override
    public String get(String key) {
        return MessageUtil.get(key);
    }
}
