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
package io.jpom.system.init;

import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.PreLoadClass;
import cn.jiangzeyin.common.PreLoadMethod;
import io.jpom.system.JpomRuntimeException;
import io.jpom.system.ServerExtConfigBean;
import io.jpom.util.CheckPassword;
import lombok.extern.slf4j.Slf4j;

/**
 * 验证token 合法性
 *
 * @author bwcx_jzy
 * @since 2019/8/5
 */
@PreLoadClass
@Slf4j
public class CheckAuthorizeToken {

	@PreLoadMethod
	private static void check() {
		String authorizeToken = ServerExtConfigBean.getInstance().getAuthorizeToken();
		if (StrUtil.isEmpty(authorizeToken)) {
			return;
		}
		if (authorizeToken.length() < 6) {
			log.error("配置的授权token长度小于六位不生效", new JpomRuntimeException("配置的授权token长度小于六位不生效"));
			System.exit(-1);
		}
		int strength = CheckPassword.checkPasswordStrength(authorizeToken);
		if (strength != 2) {
			log.error("配置的授权token 需要包含数字，字母，符号的组合", new JpomRuntimeException("配置的授权token 需要包含数字，字母，符号的组合"));
			System.exit(-1);
		}
	}
}
