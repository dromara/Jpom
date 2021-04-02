package io.jpom.util;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import io.jpom.model.data.UserModel;
import io.jpom.system.ServerExtConfigBean;
import io.jsonwebtoken.*;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * jwt 工具类
 *
 * @author bwcx_jzy
 * @date 2020/7/25
 */
public class JwtUtil {

    /**
     * token的的加密key
     */
    private static Key KEY = null;
    private static final SignatureAlgorithm ALGORITHM = SignatureAlgorithm.HS256;

    public static final String KEY_USER_ID = "userId";

    public static Claims parseBody(String token) {
        Jws<Claims> claimsJws = Jwts.parser().setSigningKey(getKey()).parseClaimsJws(token);
        return claimsJws.getBody();
    }

    private static Key getKey() {
        if (null == KEY) {
            KEY = new SecretKeySpec(RandomUtil.randomString(10).getBytes(), ALGORITHM.getJcaName());
        }
        return KEY;
    }

    /**
     * 读取token 信息 过期也能读取
     *
     * @param token token
     * @return claims
     */
    public static Claims readBody(String token) {
        try {
            return parseBody(token);
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        } catch (Exception e) {
            DefaultSystemLog.getLog().warn("token 解析失败：" + token, e);
            return null;
        }
    }

    /**
     * 读取用户id
     *
     * @param claims claims
     * @return 用户id
     */
    public static String readUserId(Claims claims) {
        return claims.get(KEY_USER_ID, String.class);
    }

    /**
     * 判断是否过期
     *
     * @param claims claims
     * @return 是否过期
     */
    public static boolean expired(Claims claims) {
        if (claims == null) {
            return true;
        }
        Date expiration = claims.getExpiration();
        if (ObjectUtil.isNotNull(expiration)) {
            return expiration.before(DateTime.now());
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
        JwtBuilder builder = Jwts.builder();
        Map<String, Object> header = new HashMap<>();
        header.put("alg", ALGORITHM.name());
        header.put("typ", "JWT");
        builder.setHeader(header);
        builder.claim(KEY_USER_ID, userModel.getId());
        builder.setId(userModel.getUserMd5Key());
        builder.setIssuer("Jpom");
        int authorizeExpired = ServerExtConfigBean.getInstance().getAuthorizeExpired();
        builder.setExpiration(DateTime.now().offset(DateField.HOUR, authorizeExpired));
        builder.signWith(ALGORITHM, getKey());
        return builder.compact();
    }


}
