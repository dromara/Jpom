/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.common;

import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;

/**
 * jpom 加载事件
 * <p>
 * 保证在容器的 bean 加载完成之后
 *
 * @author bwcx_jzy
 * @since 2022/12/25
 */
public interface ILoadEvent extends Ordered {

    /**
     * 初始化成功后执行
     *
     * @param applicationContext 应用上下文
     * @throws Exception 异常
     */
    void afterPropertiesSet(ApplicationContext applicationContext) throws Exception;

    /**
     * 排序只
     *
     * @return 0 是默认
     */
    @Override
    default int getOrder() {
        return 0;
    }
}
