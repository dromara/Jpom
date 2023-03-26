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
package io.jpom.oauth2;
import io.jpom.system.ServerConfig;
import io.jpom.system.ServerConfig.OauthConfig;
import me.zhyd.oauth.config.AuthSource;
import me.zhyd.oauth.request.AuthDefaultRequest;

public class Oauth2AuthSource implements AuthSource{

	ServerConfig.OauthConfig oauthConfig;
	
	public Oauth2AuthSource(OauthConfig oauthConfig) {
		super();
		this.oauthConfig = oauthConfig;
	}

	@Override
	public String authorize() {
		return oauthConfig.getAuthorizationUri();
	}

	@Override
	public String accessToken() {
		return oauthConfig.getAccessTokenUri();
	}

	@Override
	public String userInfo() {
		return oauthConfig.getUserInfoUri();
	}

	@Override
	public Class<? extends AuthDefaultRequest> getTargetClass() {
		// TODO Auto-generated method stub
		return AuthOauth2CustomRequest.class;
	}

}
