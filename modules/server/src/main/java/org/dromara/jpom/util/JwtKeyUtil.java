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

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.configuration.UserConfig;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * JWT 签名密钥的初始化与持久化。
 * <p>
 * 历史版本在 {@link UserConfig#getTokenJwtKeyByte()} 中使用了公开源码中的固定字符串，
 * 默认部署会使用同一把公开密钥，攻击者可在拿到密码摘要后离线伪造 JWT。
 * 这里改为：未配置 {@code jpom.user.token-jwt-key} 时，首次启动生成随机密钥并持久化到
 * {@code <data>/.jwt-key}，后续启动复用；显式配置时直接使用配置值。
 *
 * @author jpom
 * @since 2.11.12.2
 */
@Slf4j
public final class JwtKeyUtil {

    /**
     * JWT 密钥文件名（位于 jpom 数据目录下）
     */
    public static final String KEY_FILE_NAME = ".jwt-key";

    /**
     * 自动生成密钥的随机字节数（48 字节 = 384 bit）
     */
    private static final int GENERATED_KEY_BYTES = 48;

    /**
     * 建议的最小密钥长度（HS256 建议至少 256 bit，即 32 个 ASCII 字符）
     */
    private static final int RECOMMENDED_KEY_LENGTH = 32;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private JwtKeyUtil() {
    }

    /**
     * 初始化 JWT 密钥：显式配置优先，否则读取/生成 {@code <data>/.jwt-key}。
     *
     * @param userConfig 用户配置（会被写入最终使用的密钥）
     * @param dataPath   jpom 数据目录
     */
    public static void ensureKey(UserConfig userConfig, String dataPath) {
        String configured = userConfig.getTokenJwtKey();
        if (StrUtil.isNotBlank(configured)) {
            String key = configured.trim();
            if (key.length() < RECOMMENDED_KEY_LENGTH) {
                log.warn(I18nMessageUtil.get("i18n.jwt_key_length_insufficient.0c9e"), RECOMMENDED_KEY_LENGTH, RECOMMENDED_KEY_LENGTH);
            }
            userConfig.setTokenJwtKey(key);
            return;
        }
        File keyFile = FileUtil.file(dataPath, KEY_FILE_NAME);
        String key = null;
        if (FileUtil.exist(keyFile)) {
            try {
                key = StrUtil.trim(FileUtil.readString(keyFile, CharsetUtil.CHARSET_UTF_8));
            } catch (Exception e) {
                log.warn(I18nMessageUtil.get("i18n.read_jwt_key_file_failed.8493"), FileUtil.getAbsolutePath(keyFile), e);
            }
        }
        if (StrUtil.isBlank(key)) {
            byte[] randomBytes = new byte[GENERATED_KEY_BYTES];
            SECURE_RANDOM.nextBytes(randomBytes);
            key = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
            FileUtil.mkParentDirs(keyFile);
            FileUtil.writeString(key, keyFile, CharsetUtil.CHARSET_UTF_8);
            setOwnerReadWriteOnly(keyFile);
            log.warn(I18nMessageUtil.get("i18n.jwt_key_auto_generated.e061"), FileUtil.getAbsolutePath(keyFile));
        } else {
            setOwnerReadWriteOnly(keyFile);
            log.info(I18nMessageUtil.get("i18n.using_jwt_key_file.8389"), FileUtil.getAbsolutePath(keyFile));
        }
        userConfig.setTokenJwtKey(key);
    }

    /**
     * 收紧密钥文件权限为仅属主可读写（Windows 等不支持 POSIX 权限的环境忽略）。
     *
     * @param file 文件
     */
    public static void setOwnerReadWriteOnly(File file) {
        try {
            Files.setPosixFilePermissions(file.toPath(), PosixFilePermissions.fromString("rw-------"));
        } catch (UnsupportedOperationException | IOException | SecurityException e) {
            log.debug(I18nMessageUtil.get("i18n.set_file_permissions_failed.55d8"), FileUtil.getAbsolutePath(file), e);
        }
    }
}
