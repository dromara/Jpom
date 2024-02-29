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

import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationProvider;
import org.tmatesoft.svn.core.auth.ISVNProxyManager;
import org.tmatesoft.svn.core.auth.SVNAuthentication;
import org.tmatesoft.svn.core.io.SVNRepository;

import javax.net.ssl.TrustManager;
import java.util.Optional;

/**
 * svn 授权管理
 *
 * @author bwcx_jzy
 * @since 2023/2/23
 */
public class AuthenticationManager implements ISVNAuthenticationManager {

    private final ISVNAuthenticationManager delegate;
    private final Integer timeout;

    public AuthenticationManager(ISVNAuthenticationManager delegate,
                                 Integer timeout) {
        this.delegate = delegate;
        this.timeout = timeout;
    }

    @Override
    public void setAuthenticationProvider(ISVNAuthenticationProvider provider) {
        delegate.setAuthenticationProvider(provider);
    }

    @Override
    public ISVNProxyManager getProxyManager(SVNURL url) throws SVNException {
        return delegate.getProxyManager(url);
    }

    @Override
    public TrustManager getTrustManager(SVNURL url) throws SVNException {
        return delegate.getTrustManager(url);
    }

    @Override
    public SVNAuthentication getFirstAuthentication(String kind, String realm, SVNURL url) throws SVNException {
        return delegate.getFirstAuthentication(kind, realm, url);
    }

    @Override
    public SVNAuthentication getNextAuthentication(String kind, String realm, SVNURL url) throws SVNException {
        return delegate.getNextAuthentication(kind, realm, url);
    }

    @Override
    public void acknowledgeAuthentication(boolean accepted, String kind, String realm, SVNErrorMessage errorMessage, SVNAuthentication authentication) throws SVNException {
        delegate.acknowledgeAuthentication(accepted, kind, realm, errorMessage, authentication);
    }

    @Override
    public void acknowledgeTrustManager(TrustManager manager) {
        delegate.acknowledgeTrustManager(manager);
    }

    @Override
    public boolean isAuthenticationForced() {
        return delegate.isAuthenticationForced();
    }

    @Override
    public int getReadTimeout(SVNRepository repository) {
        return Optional.ofNullable(timeout)
            .map(integer -> integer <= 0 ? null : integer)
            .map(integer -> integer * 1000)
            .orElseGet(() -> delegate.getReadTimeout(repository));
    }

    @Override
    public int getConnectTimeout(SVNRepository repository) {
        return Optional.ofNullable(timeout)
            .map(integer -> integer <= 0 ? null : integer)
            .map(integer -> integer * 1000)
            .orElseGet(() -> delegate.getConnectTimeout(repository));
    }
}
