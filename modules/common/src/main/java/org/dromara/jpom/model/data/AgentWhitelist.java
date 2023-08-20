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
package org.dromara.jpom.model.data;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.StrSplitter;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.model.BaseJsonModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.system.ExtConfigBean;
import org.dromara.jpom.util.FileUtils;
import org.springframework.util.Assert;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 白名单
 *
 * @author bwcx_jzy
 * @since 2019/4/16
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentWhitelist extends BaseJsonModel {
    /**
     * 项目目录白名单、日志文件白名单
     */
    private List<String> project;
    /**
     * nginx 配置文件 白名单
     */
    private List<String> nginx;
    /**
     * nginx 安装路径
     */
    private String nginxPath;
    /**
     * 运行编辑的后缀文件
     */
    private List<String> allowEditSuffix;

    /**
     * 运行远程下载的 host
     */
    @Deprecated
    private Set<String> allowRemoteDownloadHost;

    public static String convertRealPath(String path) {
        String val = String.format("/%s/", path);
        return FileUtil.normalize(val);
    }

    public static List<String> useConvert(List<String> list) {
        if (list == null) {
            return null;
        }
        return list.stream().map(AgentWhitelist::convertRealPath).collect(Collectors.toList());
    }

    public List<String> nginx() {
        return useConvert(nginx);
    }

    public List<String> project() {
        return useConvert(project);
    }

    /**
     * 格式化，判断是否与jpom 数据路径冲突
     *
     * @param list list
     * @return null 是有冲突的
     */
    public static List<String> covertToArray(List<String> list, String errorMsg) {
        if (list == null) {
            return null;
        }
        return list.stream()
            .map(s -> {
                String val = String.format("/%s/", s);
                val = FileUtil.normalize(val);
                FileUtils.checkSlip(val);
                // 判断是否保护jpom 路径
                Assert.state(!StrUtil.startWith(ExtConfigBean.getPath(), val), errorMsg);
                return val;
            })
            .distinct()
            .collect(Collectors.toList());
    }

    /**
     * 转换为字符串
     *
     * @param jsonArray jsonArray
     * @return str
     */
    public static String convertToLine(Collection<String> jsonArray) {
        return CollUtil.join(jsonArray, StrUtil.CRLF);
    }

    /**
     * 判断是否在白名单列表中
     *
     * @param list list
     * @param path 对应项
     * @return false 不在列表中
     */
    public static boolean checkPath(List<String> list, String path) {
        if (list == null) {
            return false;
        }
        if (StrUtil.isEmpty(path)) {
            return false;
        }
        File file1, file2 = FileUtil.file(convertRealPath(path));
        for (String item : list) {
            file1 = FileUtil.file(item);
            if (FileUtil.pathEquals(file1, file2)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 将字符串转为 list
     *
     * @param value    字符串
     * @param errorMsg 错误消息
     * @return list
     */
    public static List<String> parseToList(String value, String errorMsg) {
        return parseToList(value, false, errorMsg);
    }

    /**
     * 将字符串转为 list
     *
     * @param value    字符串
     * @param required 是否为必填
     * @param errorMsg 错误消息
     * @return list
     */
    public static List<String> parseToList(String value, boolean required, String errorMsg) {
        if (required) {
            Assert.hasLength(value, errorMsg);
        } else {
            if (StrUtil.isEmpty(value)) {
                return null;
            }
        }
        List<String> list = StrSplitter.splitTrim(value, StrUtil.LF, true);
        Assert.notEmpty(list, errorMsg);
        return list;
    }

    /**
     * 获取文件可以编辑的 文件编码格式
     *
     * @param filename 文件名
     * @return charset 不能编辑情况会抛出异常
     */
    public static Charset checkFileSuffix(List<String> allowEditSuffix, String filename) {
        Assert.notEmpty(allowEditSuffix, "没有配置可允许编辑的后缀");
        Charset charset = AgentWhitelist.parserFileSuffixMap(allowEditSuffix, filename);
        Assert.notNull(charset, "不允许编辑的文件后缀");
        return charset;
    }

    /**
     * 静默判断是否可以编辑对应的文件
     *
     * @param filename 文件名
     * @return true 可以编辑
     */
    public static boolean checkSilentFileSuffix(List<String> allowEditSuffix, String filename) {
        if (CollUtil.isEmpty(allowEditSuffix)) {
            return false;
        }
        Charset charset = AgentWhitelist.parserFileSuffixMap(allowEditSuffix, filename);
        return charset != null;
    }

    /**
     * 根据文件名 和 可以配置列表 获取编码格式
     *
     * @param allowEditSuffix 允许编辑的配置
     * @param filename        文件名
     * @return 没有匹配到 返回 null，没有配置编码格式即使用系统默认编码格式
     */
    private static Charset parserFileSuffixMap(List<String> allowEditSuffix, String filename) {
        Map<String, Charset> map = CollStreamUtil.toMap(allowEditSuffix, s -> {
            List<String> split = StrUtil.split(s, StrUtil.AT);
            return CollUtil.getFirst(split);
        }, s -> {
            List<String> split = StrUtil.split(s, StrUtil.AT);
            if (split.size() > 1) {
                String last = CollUtil.getLast(split);
                return CharsetUtil.charset(last);
            } else {
                return CharsetUtil.defaultCharset();
            }
        });
        // 可能配置 所有
        Charset charset = map.get("*");
        if (charset != null) {
            return charset;
        }
        Set<Map.Entry<String, Charset>> entries = map.entrySet();
        for (Map.Entry<String, Charset> entry : entries) {
            if (StrUtil.endWithAnyIgnoreCase(filename, entry.getKey(), StrUtil.DOT + entry.getKey())) {
                return entry.getValue();
            }
            if (ReUtil.isMatch(entry.getKey(), filename)) {
                // 满足正则条件
                return entry.getValue();
            }
        }
        return null;
    }
}
