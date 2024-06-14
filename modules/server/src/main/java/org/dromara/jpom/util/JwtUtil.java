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

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTHeader;
import cn.hutool.jwt.JWTValidator;
import cn.hutool.jwt.signers.JWTSignerUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.configuration.UserConfig;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.system.ServerConfig;

/**
 * jwt 工具类
 *
 * @author bwcx_jzy
 * @since 2020/7/25
 */
@Slf4j
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

    public static JWT parseBody(String token) {
        if (StrUtil.isEmpty(token)) {
            return null;
        }
        ServerConfig serverConfig = SpringUtil.getBean(ServerConfig.class);
        UserConfig user = serverConfig.getUser();
        JWT jwt = JWT.of(token);
        if (jwt.verify(JWTSignerUtil.hs256(user.getTokenJwtKeyByte()))) {
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
            log.warn(I18nMessageUtil.get("i18n.token_parse_failed.cadf") + token, e);
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
    public static String builder(UserModel userModel, String jwtId) {
        ServerConfig serverConfig = SpringUtil.getBean(ServerConfig.class);
        UserConfig user = serverConfig.getUser();
        //
        DateTime now = DateTime.now();
        JWT jwt = JWT.create();
        jwt.setHeader(JWTHeader.ALGORITHM, ALGORITHM);
        jwt.setPayload(KEY_USER_ID, userModel.getId())
            .setJWTId(jwtId)
            .setIssuer("Jpom")
            .setIssuedAt(now)
            .setExpiresAt(now.offsetNew(DateField.HOUR, user.getTokenExpired()));
        return jwt.sign(JWTSignerUtil.hs256(user.getTokenJwtKeyByte()));
    }


}
