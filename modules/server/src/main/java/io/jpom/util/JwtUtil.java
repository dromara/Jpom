//package io.jpom.util;
//
//import io.jsonwebtoken.*;
//
//import javax.crypto.spec.SecretKeySpec;
//import java.security.Key;
//
///**
// * jwt 工具类
// *
// * @author bwcx_jzy
// * @date 2020/7/25
// */
//public class JwtUtil {
//
//
//    /**
//     * token的的加密key
//     */
//    public static final Key KEY = new SecretKeySpec("1Xh4wFQbapK7jWeb".getBytes(), SignatureAlgorithm.HS256.getJcaName());
//
//    /**
//     * 刷新token 的加密key
//     */
//    public static final Key REFRESH_KEY = new SecretKeySpec("huJG7EjLfEuiS0xB".getBytes(), SignatureAlgorithm.HS512.getJcaName());
//
//
//    public static final String KEY_USER_ID = "userId";
//
//    public static Claims parseBody(String token) {
//        Jws<Claims> claimsJws = Jwts.parser().setSigningKey(KEY).parseClaimsJws(token);
//        return claimsJws.getBody();
//    }
//
//    /**
//     * 读取token 信息 过期也能读取
//     *
//     * @param token token
//     * @return claims
//     */
//    public static Claims readBody(String token) {
//        try {
//            return parseBody(token);
//        } catch (ExpiredJwtException e) {
//            return e.getClaims();
//        }
//    }
//
//    /**
//     * 读取用户id
//     *
//     * @param token token
//     * @return 用户id
//     */
//    public static String readUserId(String token) {
//        Claims claims = readBody(token);
//        return claims.get(KEY_USER_ID, String.class);
//    }
//}
