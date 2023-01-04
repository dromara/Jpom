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
package io.jpom.util;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONValidator;
import org.springframework.core.env.SimpleCommandLinePropertySource;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
     * @param file      要删除的文件
     * @param startPath 开始的路径
     * @param inName    是否返回文件名
     * @return /test/a.txt /test/  a.txt
     */
    public static String delStartPath(File file, String startPath, boolean inName) {
        String newWhitePath;
        if (inName) {
            newWhitePath = FileUtil.getAbsolutePath(file.getAbsolutePath());
        } else {
            newWhitePath = FileUtil.getAbsolutePath(file.getParentFile());
        }
        String itemAbsPath = FileUtil.getAbsolutePath(startPath);
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
     * 指定时间的下一个刻度
     *
     * @return String
     */
    public static String getNextScaleTime(String time, Long millis) {
        DateTime dateTime = DateUtil.parse(time);
        if (millis == null) {
            millis = 30 * 1000L;
        }
        DateTime newTime = dateTime.offsetNew(DateField.SECOND, (int) (millis / 1000));
        return DateUtil.formatTime(newTime);
    }

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
     * @param command 字符串
     * @param evn     map
     * @return 替换后
     */
    public static String formatStrByMap(String command, Map<String, String> evn) {
        String replace = command;
        Set<Map.Entry<String, String>> entries = evn.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            //replace = StrUtil.replace(replace, StrUtil.format("#{{}}", entry.getKey()), entry.getValue());
            replace = StrUtil.replace(replace, StrUtil.format("${{}}", entry.getKey()), entry.getValue());
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
        JSONValidator from;
        try {
            from = JSONValidator.from(json);
        } catch (JSONException jsonException) {
            return null;
        } catch (Exception e) {
            // ArrayIndexOutOfBoundsException
            return null;
        }
        return Optional.ofNullable(from).map(JSONValidator::getType).orElse(null);
    }
}
