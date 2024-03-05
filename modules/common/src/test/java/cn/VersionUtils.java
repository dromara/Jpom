/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package cn;

import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.security.CodeSource;

/**
 * VersionUtils.
 * Just read MANIFEST.MF file and find Implementation-Version property
 *
 * @author Hotstrip
 */
@Slf4j
public final class VersionUtils {

    private static final String VERSION = getVersion(VersionUtils.class, "1.0.0");

    private VersionUtils() {
    }

    /**
     * Gets version.
     *
     * @return the version
     */
    public static String getVersion() {
        return VERSION;
    }

    /**
     * Gets version.
     *
     * @param cls            the cls
     * @param defaultVersion the default version
     * @return the version
     */
    public static String getVersion(final Class<?> cls, final String defaultVersion) {
        // find version info from MANIFEST.MF first
        String version = cls.getPackage().getImplementationVersion();
        if (StrUtil.isEmpty(version)) {
            version = cls.getPackage().getSpecificationVersion();
        }
        if (!StrUtil.isEmpty(version)) {
            return version;
        }
        // guess version fro jar file name if nothing's found from MANIFEST.MF
        CodeSource codeSource = cls.getProtectionDomain().getCodeSource();

        if (codeSource == null) {
            log.warn("No codeSource for class {} when getVersion, use default version {}", cls.getName(), defaultVersion);
            return defaultVersion;
        }
        String file = codeSource.getLocation().getFile();
        if (file != null && file.endsWith(FileNameUtil.EXT_JAR)) {
            file = file.substring(0, file.length() - 4);
            int i = file.lastIndexOf('/');
            if (i >= 0) {
                file = file.substring(i + 1);
            }
            i = file.indexOf("-");
            if (i >= 0) {
                file = file.substring(i + 1);
            }
            while (file.length() > 0 && !Character.isDigit(file.charAt(0))) {
                i = file.indexOf("-");
                if (i < 0) {
                    break;
                }
                file = file.substring(i + 1);
            }
            version = file;
        }
        // return default version if no version info is found
        return StrUtil.isEmpty(version) ? defaultVersion : version;
    }
}


