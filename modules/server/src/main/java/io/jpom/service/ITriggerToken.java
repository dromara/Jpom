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
package io.jpom.service;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.db.Entity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 带有触发器 token 相关实现服务
 *
 * @author bwcx_jzy
 * @since 2022/7/22
 */
public interface ITriggerToken {

    /**
     * 类型 名称
     *
     * @return 数据分类名称
     */
    String typeName();

    /**
     * 当前数据所有 的 token
     *
     * @return map key=ID，value=token
     */
    default Map<String, String> allTokens() {
        List<Entity> entities = this.allEntityTokens();
        return Optional.ofNullable(entities)
            .map(entities1 -> CollStreamUtil.toMap(entities1, entity -> entity.getStr("id"), entity -> entity.getStr("triggerToken")))
            .orElse(null);
    }

    /**
     * 当前数据所有 的 token
     *
     * @return list Entity
     */
    List<Entity> allEntityTokens();
}
