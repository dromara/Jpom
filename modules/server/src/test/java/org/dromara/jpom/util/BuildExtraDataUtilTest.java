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

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.TestI18nSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 构建扩展数据（extraData）解析的行为级回归。
 * <p>
 * 反证要求：把实现改回裸 {@code JSON.parseObject(extraData)}（不判空、不兜底 null）时，
 * {@link #blankReturnsWritableObject()} 必然失败——这正是 2.11.15 之前
 * {@code POST /build/edit} 不传 extraData 就 500 空指针的成因。
 * </p>
 *
 * @author jpom
 */
public class BuildExtraDataUtilTest {

    @Test
    public void blankReturnsWritableObject() {
        TestI18nSupport.init();
        JSONObject nullInput = BuildExtraDataUtil.parse(null);
        JSONObject emptyInput = BuildExtraDataUtil.parse("");
        JSONObject blankInput = BuildExtraDataUtil.parse("   ");
        assertNotNull(nullInput);
        assertNotNull(emptyInput);
        assertNotNull(blankInput);
        // 必须可写：调用方随后会 put releaseMethodDataId 等字段
        nullInput.put("releaseMethodDataId", "LocalCommand");
        assertTrue(nullInput.containsKey("releaseMethodDataId"));
        // 旧实现（裸调用）在这里得到的是 null，反证失败点
        assertNull(JSON.parseObject((String) null));
    }

    @Test
    public void jsonNullTextReturnsWritableObject() {
        TestI18nSupport.init();
        JSONObject jsonObject = BuildExtraDataUtil.parse("null");
        assertNotNull(jsonObject);
        jsonObject.put("releaseMethodDataId", "LocalCommand");
        assertTrue(jsonObject.containsKey("releaseMethodDataId"));
    }

    @Test
    public void normalObjectKept() {
        TestI18nSupport.init();
        JSONObject jsonObject = BuildExtraDataUtil.parse("{\"projectSecondaryDirectory\":\"target\"}");
        assertEquals("target", jsonObject.getString("projectSecondaryDirectory"));
    }

    @Test
    public void invalidTextTurnsIntoBusinessError() {
        TestI18nSupport.init();
        assertThrows(IllegalArgumentException.class, () -> BuildExtraDataUtil.parse("abc"));
    }
}
