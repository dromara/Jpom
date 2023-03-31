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

import org.dromara.jpom.model.BaseUserModifyDbModel;
import org.dromara.jpom.util.StringUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.jpom.db.TableName;

import java.util.List;

/**
 * 系统参数
 *
 * @author bwcx_jzy
 * @since 2021/12/2
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "SYSTEM_PARAMETERS", name = "系统参数")
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
