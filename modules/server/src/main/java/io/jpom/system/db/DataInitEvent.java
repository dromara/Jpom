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
package io.jpom.system.db;

import cn.hutool.extra.spring.SpringUtil;
import io.jpom.common.ILoadEvent;
import io.jpom.service.h2db.BaseGroupService;
import io.jpom.service.h2db.BaseNodeGroupService;
import io.jpom.service.h2db.BaseNodeService;
import org.springframework.context.ApplicationContext;

import java.util.Map;

/**
 * 数据库初始化完成后
 *
 * @author bwcx_jzy
 * @since 2023/2/18
 */
public class DataInitEvent implements ILoadEvent {
    @Override
    public void afterPropertiesSet(ApplicationContext applicationContext) throws Exception {
        //
        Map<String, BaseGroupService> groupServiceMap = SpringUtil.getApplicationContext().getBeansOfType(BaseGroupService.class);
        for (BaseGroupService<?> value : groupServiceMap.values()) {
            value.repairGroupFiled();
        }
        //
        Map<String, BaseNodeGroupService> nodeGroupServiceMap = SpringUtil.getApplicationContext().getBeansOfType(BaseNodeGroupService.class);
        for (BaseNodeGroupService<?> value : nodeGroupServiceMap.values()) {
            value.repairGroupFiled();
        }
        //  同步项目
        Map<String, BaseNodeService> beansOfType = SpringUtil.getApplicationContext().getBeansOfType(BaseNodeService.class);
        for (BaseNodeService<?> value : beansOfType.values()) {
            value.syncAllNode();
        }
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE + 2;
    }
}
