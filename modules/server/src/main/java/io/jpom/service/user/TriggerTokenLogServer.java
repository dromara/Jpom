package io.jpom.service.user;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestAlgorithm;
import cn.hutool.crypto.digest.Digester;
import cn.jiangzeyin.common.spring.SpringUtil;
import io.jpom.controller.build.BuildInfoTriggerController;
import io.jpom.model.BaseIdModel;
import io.jpom.model.data.UserModel;
import io.jpom.model.user.TriggerTokenLogBean;
import io.jpom.service.IStatusRecover;
import io.jpom.service.ITriggerToken;
import io.jpom.service.h2db.BaseDbService;
import io.jpom.service.system.SystemParametersServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author bwcx_jzy
 * @since 2022/7/22
 */
@Service
@Slf4j
public class TriggerTokenLogServer extends BaseDbService<TriggerTokenLogBean> implements IStatusRecover {

    private final SystemParametersServer parametersServer;
    private final UserService userService;

    public TriggerTokenLogServer(SystemParametersServer parametersServer,
                                 UserService userService) {
        this.parametersServer = parametersServer;
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
    public String createToken(String type, String dataId, String userId) {
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
    public int statusRecover() {
        String name = "sync_trigger_token";
        String triggerToken = parametersServer.getConfig(name, String.class);
        if (StrUtil.isNotEmpty(triggerToken)) {
            // 已经同步过啦
            return 0;
        }
        List<UserModel> list = userService.list();
        if (CollUtil.isEmpty(list)) {
            log.warn("TriggerToken status recover,user list empty");
            return -1;
        }
        List<String> userIds = list.stream().map(BaseIdModel::getId).collect(Collectors.toList());
        Map<String, ITriggerToken> beansOfType = SpringUtil.getApplicationContext().getBeansOfType(ITriggerToken.class);

        int count = beansOfType.values().stream().mapToInt(value -> {
            Map<String, String> tokens = value.allTokens();
            List<TriggerTokenLogBean> triggerTokenLogBeans = tokens.entrySet().stream().map(entry -> {
                String userId = parseUserId(userIds, entry.getValue());
                if (userId == null) {
                    return null;
                }
                TriggerTokenLogBean triggerTokenLogBean = new TriggerTokenLogBean();
                triggerTokenLogBean.setId(IdUtil.fastSimpleUUID());
                triggerTokenLogBean.setTriggerToken(entry.getValue());
                triggerTokenLogBean.setDataId(entry.getKey());
                triggerTokenLogBean.setType(value.typeName());
                triggerTokenLogBean.setUserId(userId);
                return triggerTokenLogBean;
            }).filter(Objects::nonNull).collect(Collectors.toList());
            // 插入
            this.insert(triggerTokenLogBeans);
            //
            return CollUtil.size(triggerTokenLogBeans);
        }).sum();
        parametersServer.upsert(name, count, name);
        log.info("trigger token sync count:{}", count);
        return count;
    }

    /**
     * 解析 旧版本中的 token 中的用户ID
     *
     * @param userIds 所有的用户 ID
     * @param token   token
     * @return userId
     */
    private String parseUserId(List<String> userIds, String token) {
        String digestCountStr = StrUtil.sub(token, 0, BuildInfoTriggerController.BUILD_INFO_TRIGGER_TOKEN_FILL_LEN);
        String result = StrUtil.subSuf(token, BuildInfoTriggerController.BUILD_INFO_TRIGGER_TOKEN_FILL_LEN);
        int digestCount = Convert.toInt(digestCountStr, 1);
        for (String userId : userIds) {
            String nowStr = new Digester(DigestAlgorithm.SHA256).setDigestCount(digestCount).digestHex(userId);
            if (StrUtil.equals(nowStr, result)) {
                return userId;
            }
        }
        return null;
    }
}
