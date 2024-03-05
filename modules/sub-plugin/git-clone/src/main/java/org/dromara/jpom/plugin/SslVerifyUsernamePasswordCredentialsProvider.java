/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.plugin;

import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.transport.*;

/**
 * <a href="https://stackoverflow.com/questions/33998477/turn-ssl-verification-off-for-jgit-clone-command/35587504">https://stackoverflow.com/questions/33998477/turn-ssl-verification-off-for-jgit-clone-command/35587504</a>
 *
 * @author bwcx_jzy
 * @see ChainingCredentialsProvider
 * @see HttpTransport
 * @see TransportHttp#trustInsecureSslConnection(Throwable)
 * @since 2023/1/29
 */
public class SslVerifyUsernamePasswordCredentialsProvider extends UsernamePasswordCredentialsProvider {

    public SslVerifyUsernamePasswordCredentialsProvider(String username, String password) {
        super(username, password);
    }

    @Override
    public boolean supports(CredentialItem... items) {
        for (CredentialItem item : items) {
            if ((item instanceof CredentialItem.YesNoType)) {
                return true;
            }
        }
        return super.supports(items);
    }

    @Override
    public boolean get(URIish uri, CredentialItem... items) throws UnsupportedCredentialItem {
        for (CredentialItem item : items) {
            if (item instanceof CredentialItem.YesNoType) {
                ((CredentialItem.YesNoType) item).setValue(true);
                return true;
            }
        }
        return super.get(uri, items);
    }
}
