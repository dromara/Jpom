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

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.common.i18n.I18nMessageUtil;

/**
 * 构建配置扩展数据（extraData）解析
 * <p>
 * 该参数是可选参数：页面始终提交 JSON 文本，但接口直连（脚本、第三方平台）可能不传，
 * 历史数据的 {@code extra_data} 也可能为空。裸调用 {@code JSON.parseObject(null)} 会返回 {@code null}，
 * 随后一取字段就是空指针 → 接口 500，因此统一在这里收口：
 * 空输入返回可写的空对象，非法输入转业务异常，调用方拿到的永远是可用对象。
 * </p>
 *
 * @author jpom
 * @since 2026-11-24
 */
public final class BuildExtraDataUtil {

    private BuildExtraDataUtil() {
    }

    /**
     * 解析构建扩展数据
     *
     * @param extraData 原始 JSON 文本，允许为空（未传、历史数据为空）
     * @return 非 null 且可写的 JSON 对象
     */
    public static JSONObject parse(String extraData) {
        if (StrUtil.isBlank(extraData)) {
            return new JSONObject();
        }
        JSONObject jsonObject;
        try {
            jsonObject = JSON.parseObject(extraData);
        } catch (JSONException e) {
            // 非法 JSON 文本：给出可读的业务提示，不让解析异常以 500 回显给调用方
            throw new IllegalArgumentException(I18nMessageUtil.get("i18n.correct_information_required.5e12"), e);
        }
        // JSON.parseObject("null") 同样得到 null，保持「返回可用对象」的契约
        return jsonObject == null ? new JSONObject() : jsonObject;
    }
}
