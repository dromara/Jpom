/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package com.example.twofactorauthdemo;

import org.databene.contiperf.PerfTest;
import org.databene.contiperf.junit.ContiPerfRule;
import org.dromara.jpom.util.TwoFactorAuthUtils;
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
