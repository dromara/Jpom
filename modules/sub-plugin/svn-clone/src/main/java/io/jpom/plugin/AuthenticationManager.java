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
package io.jpom.plugin;

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
