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
		String tfaCode = TwoFactorAuthUtils.generateTFACode(tfaKey);
		String generateOtpAuthUrl = TwoFactorAuthUtils.generateOtpAuthUrl("adin", tfaKey);
		System.out.println(generateOtpAuthUrl);

		boolean validateTFACode = TwoFactorAuthUtils.validateTFACode(tfaKey, tfaCode);
		System.out.println(tfaCode);
		Assert.state(validateTFACode, "验证失败");
	}
}
