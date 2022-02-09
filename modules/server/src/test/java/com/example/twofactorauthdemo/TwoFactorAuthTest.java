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
package com.example.twofactorauthdemo;

import io.jpom.util.TwoFactorAuthUtils;
import org.databene.contiperf.PerfTest;
import org.databene.contiperf.junit.ContiPerfRule;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.util.Assert;

/**
 * @author bwcx_jzy
 * @since 2022/1/27
 */
public class TwoFactorAuthTest {
	private String tfaKey;

	@Rule
	public ContiPerfRule i = ContiPerfRule.createDefaultRule();

	//	@Before
	public void before() {
		//tfaKey = TwoFactorAuthUtils.generateTFAKey();
		//System.out.println(tfaKey);
	}

	@PerfTest(invocations = 2000000, threads = 16)
	@Test
	public void test1() {
		String tfaKey = TwoFactorAuthUtils.generateTFAKey();
		String tfaCode = TwoFactorAuthUtils.generateTFACode(tfaKey);

		boolean validateTFACode = TwoFactorAuthUtils.validateTFACode(tfaKey, tfaCode);
		System.out.println(tfaKey + "  " + tfaCode + "  " + Thread.currentThread().getName());
		Assert.state(validateTFACode, "验证失败:" + tfaCode);
	}

	@Test
	public void test() {

		String generateOtpAuthUrl = TwoFactorAuthUtils.generateOtpAuthUrl("jpom@jpom.com", tfaKey);
		System.out.println(generateOtpAuthUrl);

		String tfaCode = TwoFactorAuthUtils.generateTFACode(tfaKey);

		boolean validateTFACode = TwoFactorAuthUtils.validateTFACode(tfaKey, tfaCode);
		System.out.println(tfaCode);
		Assert.state(validateTFACode, "验证失败");
	}
}
