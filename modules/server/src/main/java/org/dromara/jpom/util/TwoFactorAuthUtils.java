/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.util;

import cn.hutool.core.codec.Base32;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.HMac;

/**
 * @author bwcx_jzy
 * @since 2022/1/27
 */
public class TwoFactorAuthUtils {

	private static final int VALID_TFA_WINDOW_MILLIS = 60_000;


	/**
	 * 生成两步验证 Key
	 *
	 * @return 两步验证 Key
	 */
	public static String generateTFAKey() {
		return TimeBasedOneTimePasswordUtil.generateBase32Secret(32);
	}

	/**
	 * 生成两步验证码
	 *
	 * @param tfaKey 两步验证 Key
	 * @return 两步验证码
	 */
	public static String generateTFACode(String tfaKey) {
		return TimeBasedOneTimePasswordUtil.generateCurrentNumberString(tfaKey);
	}

	/**
	 * 验证两步验证码
	 *
	 * @param tfaKey  两步验证 Key
	 * @param tfaCode 两步验证码
	 */
	public static boolean validateTFACode(String tfaKey, String tfaCode) {
		int validCode = Convert.toInt(tfaCode, 0);
		return TimeBasedOneTimePasswordUtil.validateCurrentNumber(tfaKey, validCode, VALID_TFA_WINDOW_MILLIS);
	}

	/**
	 * 生成 Otp Auth Url
	 *
	 * @param userName 用户名
	 * @param tfaKey   两步验证 Key
	 * @return URL
	 */
	public static String generateOtpAuthUrl(String userName, final String tfaKey) {
		String jpomName = "jpom-" + userName;
		return TimeBasedOneTimePasswordUtil.generateOtpAuthUrl(jpomName, tfaKey);
	}

	private static class TimeBasedOneTimePasswordUtil {
		public static final int DEFAULT_TIME_STEP_SECONDS = 30;
		private static final int NUM_DIGITS_OUTPUT = 6;

		public static String generateBase32Secret(int length) {
			return RandomUtil.randomString(RandomUtil.BASE_CHAR, length).toUpperCase();
		}

		public static boolean validateCurrentNumber(String base32Secret, int authNumber,
													int windowMillis) {
			return validateCurrentNumber(base32Secret, authNumber, windowMillis,
					System.currentTimeMillis(),
					DEFAULT_TIME_STEP_SECONDS);
		}

		public static boolean validateCurrentNumber(String base32Secret, int authNumber,
													int windowMillis, long timeMillis,
													int timeStepSeconds) {
			long fromTimeMillis = timeMillis;
			long toTimeMillis = timeMillis;
			if (windowMillis > 0) {
				fromTimeMillis -= windowMillis;
				toTimeMillis += windowMillis;
			}
			long timeStepMillis = timeStepSeconds * 1000L;
			for (long millis = fromTimeMillis; millis <= toTimeMillis; millis += timeStepMillis) {
				int generatedNumber = generateNumber(base32Secret, millis, timeStepSeconds);
				if (generatedNumber == authNumber) {
					return true;
				}
			}
			return false;
		}

		public static String generateCurrentNumberString(String base32Secret) {
			return generateNumberString(base32Secret, System.currentTimeMillis(), DEFAULT_TIME_STEP_SECONDS);
		}

		public static String generateNumberString(String base32Secret, long timeMillis, int timeStepSeconds) {
			int number = generateNumber(base32Secret, timeMillis, timeStepSeconds);
			String numStr = Integer.toString(number);
			return StrUtil.fillBefore(numStr, '0', TimeBasedOneTimePasswordUtil.NUM_DIGITS_OUTPUT);
		}

		public static int generateNumber(String base32Secret, long timeMillis, int timeStepSeconds) {

			byte[] key = Base32.decode(base32Secret);

			byte[] data = new byte[8];
			long value = timeMillis / 1000 / timeStepSeconds;
			for (int i = 7; value > 0; i--) {
				data[i] = (byte) (value & 0xFF);
				value >>= 8;
			}
			HMac hMac = SecureUtil.hmacSha1(key);
			byte[] hash = hMac.digest(data);

			// take the 4 least significant bits from the encrypted string as an offset
			int offset = hash[hash.length - 1] & 0xF;

			// We're using a long because Java hasn't got unsigned int.
			long truncatedHash = 0;
			for (int i = offset; i < offset + 4; ++i) {
				truncatedHash <<= 8;
				// get the 4 bytes at the offset
				truncatedHash |= hash[i] & 0xFF;
			}
			// cut off the top bit
			truncatedHash &= 0x7FFFFFFF;

			// the token is then the last 6 digits in the number
			truncatedHash %= 1000000;
			// this is only 6 digits so we can safely case it
			return (int) truncatedHash;
		}

		public static String generateOtpAuthUrl(String keyId, String secret) {
			//			addOtpAuthPart(keyId, secret, sb);
			return "otpauth://totp/" + keyId + "?secret=" + secret;
		}
	}
}
