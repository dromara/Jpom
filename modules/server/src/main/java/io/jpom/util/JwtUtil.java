package io.jpom.util;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTHeader;
import cn.hutool.jwt.JWTValidator;
import cn.hutool.jwt.signers.JWTSignerUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import io.jpom.model.data.UserModel;
import io.jpom.system.ServerExtConfigBean;

/**
 * jwt 工具类
 *
 * @author bwcx_jzy
 * @date 2020/7/25
 */
public class JwtUtil {

	/**
	 * 加密算法
	 */
	private static final String ALGORITHM = "HS256";
	/**
	 * token的的加密key
	 */
	private static byte[] KEY;
	public static final String KEY_USER_ID = "userId";

	private static byte[] getKey() {
		if (KEY == null) {
			KEY = ServerExtConfigBean.getInstance().getAuthorizeKey();
		}
		return KEY;
	}

	public static JWT parseBody(String token) {
		if (StrUtil.isEmpty(token)) {
			return null;
		}
		JWT jwt = JWT.of(token);
		if (jwt.verify(JWTSignerUtil.hs256(getKey()))) {
			return jwt;
		}
		return null;
	}


	/**
	 * 读取token 信息 过期也能读取
	 *
	 * @param token token
	 * @return claims
	 */
	public static JWT readBody(String token) {
		try {
			return parseBody(token);
		} catch (Exception e) {
			DefaultSystemLog.getLog().warn("token 解析失败：" + token, e);
			return null;
		}
	}

	/**
	 * 读取用户id
	 *
	 * @param jwt jwt
	 * @return 用户id
	 */
	public static String readUserId(JWT jwt) {
		return Convert.toStr(jwt.getPayload(KEY_USER_ID));
	}

	/**
	 * 获取jwt的唯一身份标识
	 *
	 * @param jwt jwt
	 * @return id
	 */
	public static String getId(JWT jwt) {
		if (null == jwt) {
			return null;
		}
		return Convert.toStr(jwt.getPayload(JWT.JWT_ID));
	}

	/**
	 * 判断是否过期
	 *
	 * @param jwt    claims
	 * @param leeway 容忍空间，单位：秒。当不能晚于当前时间时，向后容忍；不能早于向前容忍。
	 * @return 是否过期
	 */
	public static boolean expired(JWT jwt, long leeway) {
		if (jwt == null) {
			return true;
		}
		try {
			JWTValidator of = JWTValidator.of(jwt);
			of.validateDate(DateUtil.date(), leeway);
		} catch (Exception e) {
			return true;
		}
		return false;
	}

	/**
	 * 生成token
	 *
	 * @param userModel 用户
	 * @return token
	 */
	public static String builder(UserModel userModel) {
		int authorizeExpired = ServerExtConfigBean.getInstance().getAuthorizeExpired();
		DateTime now = DateTime.now();
		JWT jwt = JWT.create();
		jwt.setHeader(JWTHeader.ALGORITHM, ALGORITHM);
		jwt.setPayload(KEY_USER_ID, userModel.getId())
				.setJWTId(userModel.getUserMd5Key())
				.setIssuer("Jpom")
				.setIssuedAt(now)
				.setExpiresAt(now.offsetNew(DateField.HOUR, authorizeExpired));
		return jwt.sign(JWTSignerUtil.hs256(getKey()));
	}


}
