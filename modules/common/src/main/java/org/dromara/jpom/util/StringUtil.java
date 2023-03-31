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
package org.dromara.jpom.util;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.cron.pattern.CronPattern;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONValidator;
import org.springframework.core.env.SimpleCommandLinePropertySource;

import java.io.File;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * main 方法运行参数工具
 *
 * @author jiangzeyin
 * @see SimpleCommandLinePropertySource
 * @since 2019/4/7
 */
public class StringUtil {

    public static final String GENERAL_STR = "^[a-zA-Z0-9_\\-]+$";

    /**
     * 支持的压缩包格式
     */
    public static final String[] PACKAGE_EXT = new String[]{"tar.bz2", "tar.gz", "tar", "bz2", "zip", "gz"};

    /**
     * 转换 文件内容
     *
     * @param text 字符串，可能为文件协议地址
     * @param def  默认值
     * @return 如果存在文件 则读取文件内容
     */
    public static String convertFileStr(String text, String def) {
        if (StrUtil.startWith(text, URLUtil.FILE_URL_PREFIX)) {
            String path = StrUtil.removePrefix(text, URLUtil.FILE_URL_PREFIX);
            if (FileUtil.isFile(path)) {
                String fileText = FileUtil.readUtf8String(path);
                return StrUtil.emptyToDefault(fileText, def);
            }
        }
        return StrUtil.emptyToDefault(text, def);
    }

    /**
     * 删除文件开始的路径
     *
     * @param file     要删除的文件
     * @param baseFile 开始的路径
     * @param inName   是否返回文件名
     * @return /test/a.txt /test/  a.txt
     */
    public static String delStartPath(File file, File baseFile, boolean inName) {
        FileUtil.checkSlip(baseFile, file);
        //
        String newWhitePath;
        if (inName) {
            newWhitePath = FileUtil.getAbsolutePath(file.getAbsolutePath());
        } else {
            newWhitePath = FileUtil.getAbsolutePath(file.getParentFile());
        }
        String itemAbsPath = FileUtil.getAbsolutePath(baseFile);
        itemAbsPath = FileUtil.normalize(itemAbsPath);
        newWhitePath = FileUtil.normalize(newWhitePath);
        String path = StrUtil.removePrefix(newWhitePath, itemAbsPath);
        //newWhitePath.substring(newWhitePath.indexOf(itemAbsPath) + itemAbsPath.length());
        path = FileUtil.normalize(path);
        if (path.startsWith(StrUtil.SLASH)) {
            path = path.substring(1);
        }
        return path;
    }

    /**
     * 删除文件开始的路径
     *
     * @param file      要删除的文件
     * @param startPath 开始的路径
     * @param inName    是否返回文件名
     * @return /test/a.txt /test/  a.txt
     */
    public static String delStartPath(File file, String startPath, boolean inName) {
        return delStartPath(file, FileUtil.file(startPath), inName);
    }

//    /**
//     * 指定时间的下一个刻度
//     *
//     * @return String
//     */
//    public static String getNextScaleTime(String time, Long millis) {
//        DateTime dateTime = DateUtil.parse(time);
//        if (millis == null) {
//            millis = 30 * 1000L;
//        }
//        DateTime newTime = dateTime.offsetNew(DateField.SECOND, (int) (millis / 1000));
//        return DateUtil.formatTime(newTime);
//    }

    /**
     * json 字符串转 bean，兼容普通json和字符串包裹情况
     *
     * @param jsonStr json 字符串
     * @param cls     要转为bean的类
     * @param <T>     泛型
     * @return data
     */
    public static <T> T jsonConvert(String jsonStr, Class<T> cls) {
        if (StrUtil.isEmpty(jsonStr)) {
            return null;
        }
        if (ClassUtil.isPrimitiveWrapper(cls)) {
            return Convert.convert(cls, jsonStr);
        }
        try {
            return JSON.parseObject(jsonStr, cls);
        } catch (Exception e) {
            return JSON.parseObject(JSON.parse(jsonStr).toString(), cls);
        }
    }

    /**
     * json 字符串转 bean，兼容普通json和字符串包裹情况
     *
     * @param jsonStr json 字符串
     * @param cls     要转为bean的类
     * @param <T>     泛型
     * @return data
     */
    public static <T> List<T> jsonConvertArray(String jsonStr, Class<T> cls) {
        try {
            if (StrUtil.isEmpty(jsonStr)) {
                return null;
            }
            return JSON.parseArray(jsonStr, cls);
        } catch (Exception e) {
            Object parse = JSON.parse(jsonStr);
            return JSON.parseArray(parse.toString(), cls);
        }
    }

    /**
     * 根据 map 替换 字符串变量
     *
     * @param str 字符串
     * @param evn map
     * @return 替换后
     */
    public static String formatStrByMap(String str, Map<String, String> evn) {
        String replace = str;
        Set<Map.Entry<String, String>> entries = evn.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            replace = StrUtil.replace(replace, StrUtil.format("${{}}", entry.getKey()), entry.getValue());
            replace = StrUtil.replace(replace, StrUtil.format("${}", entry.getKey()), entry.getValue());
        }
        return replace;
    }

    /**
     * 验证 json 类型
     *
     * @param json json 字符串
     * @return type
     */
    public static JSONValidator.Type validatorJson(String json) {
        try {
            JSONValidator from = JSONValidator.from(json);
            return Optional.of(from).map(JSONValidator::getType).orElse(null);
        } catch (JSONException jsonException) {
            return null;
        } catch (Exception e) {
            // ArrayIndexOutOfBoundsException
            return null;
        }
    }

    /**
     * 验证 cron 表达式, demo 账号不能开启 cron
     *
     * @param cron cron
     * @return 原样返回
     */
    public static String checkCron(String cron, Function<String, String> function) {
        String newCron = cron;
        if (StrUtil.isNotEmpty(newCron)) {
            if (StrUtil.startWith(newCron, "!")) {
                // 不用验证
                return newCron;
            }
            newCron = function.apply(newCron);
            try {
                new CronPattern(newCron);
            } catch (Exception e) {
                throw new IllegalArgumentException("cron 表达式格式不正确");
            }
        }
        return ObjectUtil.defaultIfNull(newCron, StrUtil.EMPTY);
    }

    /**
     * 转换 cron 表达式,格式化
     *
     * @param cron cron
     * @return 转化后的
     */
    public static String parseCron(String cron) {
        if (StrUtil.startWith(cron, "!")) {
            // 存在前缀，直接返回空串
            return null;
        }
        return cron;
    }

    public static Map<String, String> parseEnvStr(String envStr) {
        List<String> list = StrUtil.splitTrim(envStr, StrUtil.LF);
        return parseEnvStr(list);
    }

    public static Map<String, String> parseEnvStr(List<String> envStrList) {
        if (envStrList == null) {
            return MapUtil.newHashMap();
        }
        List<Tuple> collect = envStrList.stream()
                .map(StrUtil::trim)
                .filter(s -> !StrUtil.isEmpty(s) && !StrUtil.startWith(s, "#"))
                .map(s -> {
                    List<String> list1 = StrUtil.splitTrim(s, "=");
                    if (CollUtil.size(list1) != 2) {
                        return null;
                    }
                    return new Tuple(list1.get(0), list1.get(1));
                }).filter(Objects::nonNull).collect(Collectors.toList());
        return CollStreamUtil.toMap(collect, objects -> objects.get(0), objects -> objects.get(1));
    }
}
