/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.model.data;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.jpom.db.TableName;
import org.dromara.jpom.model.BaseUserModifyDbModel;
import org.dromara.jpom.util.StringUtil;

import java.util.List;

/**
 * 系统参数
 *
 * @author bwcx_jzy
 * @since 2021/12/2
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "SYSTEM_PARAMETERS",
    nameKey = "i18n.system_parameters.c7b0")
@Data
public class SystemParametersModel extends BaseUserModifyDbModel {

    /**
     * 参数值
     */
    private String value;
    /**
     * 参数描述
     */
    private String description;

    public <T> T jsonToBean(Class<T> cls) {
        return StringUtil.jsonConvert(this.getValue(), cls);
    }

    public <T> List<T> jsonToBeanList(Class<T> cls) {
        return StringUtil.jsonConvertArray(this.getValue(), cls);
    }
}
