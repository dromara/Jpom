package io.jpom.util;

/**
 * 判断密码强度
 *
 * @author jiangzeyin
 * @date 2019/3/18
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
	public static int checkPassword(String passwordStr) {
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
