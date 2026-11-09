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
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;

import java.net.URL;

/**
 * 远程下载文件名称工具。
 *
 * <p>远程下载到项目 lib 目录的文件名取 URL 中的文件名（basename），保证 Java/Go 插件端保存后的
 * 文件列表、文件对比和备份目录一致。URL 为空、无法解析、basename 为空、包含路径穿越或非法字符时
 * 回退为随机名称，避免下载成功但落盘失败。
 *
 * @author Jpom
 * @since 2.11.13.2
 */
public final class RemoteFileNameUtil {

    /**
     * Windows 下不能作为文件名的字符。所有平台统一拒绝，保证同一 URL 在不同平台保存的文件名一致。
     */
    private static final String ILLEGAL_FILE_NAME_CHARS = "<>:\"|?*";

    private RemoteFileNameUtil() {
    }

    /**
     * 解析远程 URL 中用于保存到 lib 目录的文件名（URL basename）。
     *
     * @param url 远程地址
     * @return 合法的文件 basename，无法得到合法文件名时返回 32 位随机名称
     */
    public static String resolveFileName(String url) {
        String fileName = getUrlBaseName(url);
        if (isInvalidFileName(fileName)) {
            return IdUtil.fastSimpleUUID();
        }
        return fileName;
    }

    /**
     * 取 URL 路径最后一段并做 URL 解码。
     *
     * <p>先取最后一段再解码，避免 %2F 等编码斜杠解码后形成路径穿越。
     */
    private static String getUrlBaseName(String url) {
        if (StrUtil.isBlank(url)) {
            return null;
        }
        try {
            URL urlObj = URLUtil.url(url);
            String protocol = urlObj.getProtocol();
            // Hutool 会把非 URL 字符串按本地文件路径解析，远程下载只支持 http/https，其它一律回退随机名称
            if (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol)) {
                return null;
            }
            String path = urlObj.getPath();
            if (StrUtil.isBlank(path)) {
                return null;
            }
            String name = FileUtil.getName(path);
            if (StrUtil.isBlank(name)) {
                return null;
            }
            return URLUtil.decode(name, CharsetUtil.CHARSET_UTF_8, false);
        } catch (Exception e) {
            // 非法 URL 由下载逻辑负责报错，这里只负责给出安全的回退名称
            return null;
        }
    }

    /**
     * 判断文件名是否不能直接落盘。
     *
     * @param fileName 文件名称
     * @return true 表示不能直接使用
     */
    static boolean isInvalidFileName(String fileName) {
        if (StrUtil.isBlank(fileName)) {
            return true;
        }
        if (".".equals(fileName) || "..".equals(fileName)) {
            return true;
        }
        if (fileName.indexOf('/') >= 0 || fileName.indexOf('\\') >= 0) {
            return true;
        }
        // Windows 会去掉文件名末尾的点和空格，统一拒绝避免跨平台行为不一致
        if (fileName.endsWith(".") || fileName.endsWith(" ")) {
            return true;
        }
        for (int i = 0; i < fileName.length(); i++) {
            char c = fileName.charAt(i);
            if (c < 0x20 || c == 0x7f) {
                return true;
            }
            if (ILLEGAL_FILE_NAME_CHARS.indexOf(c) >= 0) {
                return true;
            }
        }
        return false;
    }
}
