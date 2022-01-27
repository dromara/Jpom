/**
 * @author bwcx_jzy
 * @since 2022/1/27
 */
package com.example.twofactorauthdemo;

import io.jpom.util.TwoFactorAuthUtils;
import org.junit.Test;
import org.springframework.util.Assert;

public class TwoFactorAuthTest {


	@Test
	public void test() {
		String tfaKey = TwoFactorAuthUtils.generateTFAKey();
		System.out.println(tfaKey);
		String generateOtpAuthUrl = TwoFactorAuthUtils.generateOtpAuthUrl("jpom@jpom.com", tfaKey);
		System.out.println(generateOtpAuthUrl);

		String tfaCode = TwoFactorAuthUtils.generateTFACode(tfaKey);

		boolean validateTFACode = TwoFactorAuthUtils.validateTFACode(tfaKey, tfaCode);
		System.out.println(tfaCode);
		Assert.state(validateTFACode, "验证失败");
	}
}
