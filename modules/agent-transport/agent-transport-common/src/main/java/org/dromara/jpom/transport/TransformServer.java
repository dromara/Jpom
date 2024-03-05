/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.transport;

import com.alibaba.fastjson2.TypeReference;

/**
 * 消息转换服务
 *
 * @author bwcx_jzy
 * @since 2022/12/24
 */
public interface TransformServer {

    /**
     * 数据类型转换
     *
     * @param data           数据
     * @param tTypeReference 类型
     * @param <T>            范型
     * @return data
     */
    <T> T transform(String data, TypeReference<T> tTypeReference);

    /**
     * 数据类型转换,只返回成功的数据
     *
     * @param data   数据
     * @param tClass 类型
     * @param <T>    范型
     * @return data
     */
    <T> T transformOnlyData(String data, Class<T> tClass);

    /**
     * 转换异常
     *
     * @param e        请求的异常
     * @param nodeInfo 节点信息
     * @return 转换后的异常
     */
    default Exception transformException(Exception e, INodeInfo nodeInfo) {
        return e;
    }
}
