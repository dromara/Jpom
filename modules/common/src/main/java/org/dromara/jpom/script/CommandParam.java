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
package org.dromara.jpom.script;

import cn.hutool.core.lang.Opt;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.model.BaseJsonModel;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONValidator;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.jpom.util.StringUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 脚本参数
 *
 * @author bwcx_jzy
 * @since 2023/3/13
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CommandParam extends BaseJsonModel {
    /**
     * 参数值
     */
    private String value;
    /**
     * 描述
     */
    private String desc;

    public static String convertToParam(String defArgs) {
        JSONValidator.Type type = StringUtil.validatorJson(defArgs);
        if (type == null || type == JSONValidator.Type.Value) {
            // 旧版本的数据
            List<CommandParam> commandParams = CommandParam.convertLineStr(defArgs);
            return commandParams == null ? null : JSONObject.toJSONString(commandParams);
        } else if (type == JSONValidator.Type.Object) {
            return defArgs;
        } else {
            return defArgs;
        }
    }

    public static String toCommandLine(String params) {
        JSONValidator.Type type = StringUtil.validatorJson(params);
        if (type == null || type == JSONValidator.Type.Value) {
            // 兼容旧数据
            return params;
        }
        List<CommandParam> paramList = params(params);
        return Optional.ofNullable(paramList)
            .map(commandParams -> commandParams.stream()
                .map(CommandParam::getValue)
                .collect(Collectors.joining(StrUtil.SPACE)))
            .orElse(StrUtil.EMPTY);
    }

    public static List<String> toCommandList(String params) {
        JSONValidator.Type type = StringUtil.validatorJson(params);
        if (type == null || type == JSONValidator.Type.Value) {
            // 兼容旧数据
            return StrUtil.splitTrim(params, StrUtil.SPACE);
        }
        List<CommandParam> paramList = params(params);
        return Optional.ofNullable(paramList)
            .map(commandParams -> commandParams.stream()
                .map(CommandParam::getValue)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()))
            .orElse(Collections.emptyList());
    }

    public static List<CommandParam> params(String defParams) {
        return StringUtil.jsonConvertArray(defParams, CommandParam.class);
    }

    public static String checkStr(String str) {
        return Opt.ofBlankAble(str)
            .map(s -> {
                List<CommandParam> params = params(s);
                return JSONObject.toJSONString(params);
            }).orElse(StrUtil.EMPTY);
    }

    public static List<CommandParam> convertLineStr(String defArgs) {
        List<String> list = StrUtil.splitTrim(defArgs, StrUtil.SPACE);
        return Optional.ofNullable(list)
            .map(strings -> {
                List<CommandParam> commandParams1 = new ArrayList<>(strings.size());
                for (int i = 0; i < strings.size(); i++) {
                    CommandParam commandParam = new CommandParam();
                    commandParam.setValue(strings.get(i));
                    commandParam.setDesc("参数" + (i + 1));
                    commandParams1.add(commandParam);
                }
                return commandParams1;
            })
            .orElse(null);
    }

}
