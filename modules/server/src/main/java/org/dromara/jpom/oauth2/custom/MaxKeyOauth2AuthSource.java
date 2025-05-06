/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.oauth2.custom;

import me.zhyd.oauth.request.AuthDefaultRequest;
import org.dromara.jpom.oauth2.platform.MaxKeyOauth2Config;

/**
 * @author MaxKey
 */
public class MaxKeyOauth2AuthSource extends CommonOauth2AuthSource {

    public MaxKeyOauth2AuthSource(MaxKeyOauth2Config oauthConfig) {
        super(oauthConfig);
    }


    @Override
    public Class<? extends AuthDefaultRequest> getTargetClass() {
        // TODO Auto-generated method stub
        return MaxKeyAuthOauth2Request.class;
    }
}
