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
package io.jpom.service.user;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.jpom.model.user.TriggerTokenLogBean;
import io.jpom.model.user.UserModel;
import io.jpom.service.h2db.BaseDbService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * @author bwcx_jzy
 * @since 2022/7/22
 */
@Service
@Slf4j
public class TriggerTokenLogServer extends BaseDbService<TriggerTokenLogBean> {

    private final UserService userService;

    public TriggerTokenLogServer(UserService userService) {
        this.userService = userService;
    }

    /**
     * 通过 token 获取用户ID
     *
     * @param token token
     * @param type  数据类型
     * @return user
     */
    public UserModel getUserByToken(String token, String type) {
        TriggerTokenLogBean tokenLogBean = super.getByKey(token);
        if (tokenLogBean != null) {
            UserModel userModel = userService.getByKey(tokenLogBean.getUserId());
            if (userModel != null && StrUtil.equals(type, tokenLogBean.getType())) {
                return userModel;
            }
        }
        // 兼容旧版本数据
        TriggerTokenLogBean where = new TriggerTokenLogBean();
        where.setTriggerToken(token);
        where.setType(type);
        List<TriggerTokenLogBean> triggerTokenLogBeans = this.listByBean(where);
        return Optional.ofNullable(triggerTokenLogBeans)
            .map(CollUtil::getFirst)
            .map(triggerTokenLogBean -> userService.getByKey(triggerTokenLogBean.getUserId()))
            .orElse(null);
    }

    /**
     * 重启生成 token
     *
     * @param oldToken 之前版本 token
     * @param type     类型
     * @param dataId   数据ID
     * @param userId   用户ID
     * @return 新 token
     */
    public String restToken(String oldToken, String type, String dataId, String userId) {
        if (StrUtil.isNotEmpty(oldToken)) {
            TriggerTokenLogBean tokenLogBean = this.getByKey(oldToken);
            if (tokenLogBean != null) {
                this.delByKey(oldToken);
            } else {
                // 可能存在之前版本数据
                TriggerTokenLogBean where = new TriggerTokenLogBean();
                where.setTriggerToken(oldToken);
                where.setType(type);
                List<TriggerTokenLogBean> triggerTokenLogBeans = this.listByBean(where);
                Optional.ofNullable(triggerTokenLogBeans)
                    .ifPresent(triggerTokenLogBeans1 ->
                        triggerTokenLogBeans1.forEach(triggerTokenLogBean -> this.delByKey(triggerTokenLogBean.getId()))
                    );
            }
        }
        // 创建 token
        return this.createToken(type, dataId, userId);
    }

    /**
     * 创建新 token
     *
     * @param type   类型
     * @param dataId 数据ID
     * @param userId 用户ID
     * @return token
     */
    private String createToken(String type, String dataId, String userId) {
        TriggerTokenLogBean trigger = new TriggerTokenLogBean();
        String uuid = IdUtil.fastSimpleUUID();
        trigger.setId(uuid);
        trigger.setTriggerToken(uuid);
        trigger.setType(type);
        trigger.setDataId(dataId);
        trigger.setUserId(userId);
        this.insert(trigger);
        return uuid;
    }
}
