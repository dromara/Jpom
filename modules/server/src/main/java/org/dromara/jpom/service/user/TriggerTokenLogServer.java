/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service.user;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.date.BetweenFormatter;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.SystemClock;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.Page;
import cn.keepbx.jpom.event.ISystemTask;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.model.PageResultDto;
import org.dromara.jpom.model.user.TriggerTokenLogBean;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.service.ITriggerToken;
import org.dromara.jpom.service.h2db.BaseDbService;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author bwcx_jzy
 * @since 2022/7/22
 */
@Service
@Slf4j
public class TriggerTokenLogServer extends BaseDbService<TriggerTokenLogBean> implements ISystemTask {

    private final UserService userService;
    private final List<ITriggerToken> triggerTokens;
    private final Map<String, ITriggerToken> triggerTokenMap;

    public TriggerTokenLogServer(UserService userService,
                                 List<ITriggerToken> triggerTokens) {
        this.userService = userService;
        this.triggerTokens = triggerTokens;
        triggerTokenMap = CollStreamUtil.toMap(triggerTokens, ITriggerToken::typeName, iTriggerToken -> iTriggerToken);
    }

    /**
     * 获取类型
     *
     * @param type 类型名称
     * @return 接口
     */
    public ITriggerToken getByType(String type) {
        return MapUtil.get(triggerTokenMap, type, ITriggerToken.class);
    }

    /**
     * 删除触发器
     *
     * @param id Id
     */
    public void delete(String id) {
        TriggerTokenLogBean tokenLogBean = this.getByKey(id);
        if (tokenLogBean == null) {
            return;
        }
        ITriggerToken token = triggerTokens.stream()
            .filter(iTriggerToken -> StrUtil.equals(iTriggerToken.typeName(), tokenLogBean.getType()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(I18nMessageUtil.get("i18n.no_trigger_type_specified.5628") + tokenLogBean.getType()));
        String sql = "update " + tokenLogBean.getType() + " set triggerToken='' where id=?";
        this.execute(sql, tokenLogBean.getDataId());
        this.delByKey(id);
    }

    /**
     * 获取所有类型
     *
     * @return list
     */
    public List<JSONObject> allType() {
        return triggerTokens.stream()
            .map(iTriggerToken -> {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("name", iTriggerToken.typeName());
                jsonObject.put("desc", iTriggerToken.getDataDesc());
                return jsonObject;
            })
            .collect(Collectors.toList());
    }

    /**
     * 通过用户ID 删除数据
     *
     * @param userId 用户d
     */
    public void delByUserId(String userId) {
        TriggerTokenLogBean tokenLogBean = new TriggerTokenLogBean();
        tokenLogBean.setUserId(userId);
        this.delByBean(tokenLogBean);
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
                boolean demoUser = userModel.isDemoUser();
                Assert.state(!demoUser, I18nMessageUtil.get("i18n.user_trigger_unavailable.9866"));
                // 修改触发次数
                String sql = "update " + this.getTableName() + " set triggerCount=ifnull(triggerCount,0)+1 where id=?";
                int execute = this.execute(sql, tokenLogBean.getId());
                return userModel;
            }
        }
        //
        return null;
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
            this.delByKey(oldToken);
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

    @Override
    public void executeTask() {
        if (triggerTokens == null) {
            return;
        }
        log.debug("clean trigger token start...");
        long start = SystemClock.now();
        // 调用方法处理逻辑
        cleanTriggerToken();
        log.debug("clean trigger token end... cost time: {}", DateUtil.formatBetween(SystemClock.now() - start, BetweenFormatter.Level.MILLISECOND));
    }

    /**
     * @author Hotstrip
     * @since 2023-04-13
     */
    private void cleanTriggerToken() {
        // 统计删除条数
        int delCount = 0;
        for (ITriggerToken triggerToken : triggerTokens) {
            TriggerTokenLogBean tokenLogBean = new TriggerTokenLogBean();
            tokenLogBean.setType(triggerToken.typeName());
            try {
                int pageNumber = 1;
                while (true) {
                    Page page = new Page(pageNumber, 50);
                    Entity entity = new Entity();
                    entity.set("type", triggerToken.typeName());
                    entity.setFieldNames("id", "dataId");
                    PageResultDto<TriggerTokenLogBean> pageResult = this.listPage(entity, page);
                    if (pageResult.isEmpty()) {
                        break;
                    }
                    List<String> ids = new ArrayList<>();
                    List<TriggerTokenLogBean> result = pageResult.getResult();
                    for (TriggerTokenLogBean bean : result) {
                        //
                        String dataId = bean.getDataId();
                        if (triggerToken.exists(dataId)) {
                            continue;
                        }
                        String id = bean.getId();
                        ids.add(id);
                    }
                    // 删除 token
                    this.delByKey(ids);
                    if (pageResult.getTotalPage() <= pageNumber) {
                        break;
                    }
                    pageNumber++;
                    delCount += ids.size();
                }
            } catch (Exception e) {
                log.error(I18nMessageUtil.get("i18n.cleanup_token_exception.760e"), triggerToken.typeName(), e);
            }
        }
        if (delCount > 0) {
            log.info("clean trigger token count: {}", delCount);
        }
    }
}
