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
package io.jpom.util;

/**
 * 判断密码强度
 *
 * @author jiangzeyin
 * @since 2019/3/18
 */
public class CheckPassword {

	private static final String REGEX_Z = "\\d*";
	private static final String REGEX_S = "[a-zA-Z]+";
	private static final String REGEX_T = "\\W+$";
	private static final String REGEX_ZT = "\\D*";
	private static final String REGEX_ST = "[\\d\\W]*";
	private static final String REGEX_ZS = "\\w*";
	private static final String REGEX_ZST = "[\\w\\W]*";

	/**
	 * 密码强度
	 * Z = 字母 S = 数字 T = 特殊字符
	 *
	 * @param passwordStr 密码字符串
	 * @return 0 弱  1 中  2强
	 */
	public static int checkPasswordStrength(String passwordStr) {
		if (passwordStr.matches(REGEX_Z)) {
			return 0;
		}
		if (passwordStr.matches(REGEX_S)) {
			return 0;
		}
		if (passwordStr.matches(REGEX_T)) {
			return 0;
		}
		if (passwordStr.matches(REGEX_ZT)) {
			return 1;
		}
		if (passwordStr.matches(REGEX_ST)) {
			return 1;
		}
		if (passwordStr.matches(REGEX_ZS)) {
			return 1;
		}
		if (passwordStr.matches(REGEX_ZST)) {
			return 2;
		}
		return -1;
	}
}
