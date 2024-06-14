/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.script;

import cn.hutool.core.lang.Opt;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.model.BaseJsonModel;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONValidator;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
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
                    commandParam.setDesc(I18nMessageUtil.get("i18n.parameter.3d0a") + (i + 1));
                    commandParams1.add(commandParam);
                }
                return commandParams1;
            })
            .orElse(null);
    }

}
