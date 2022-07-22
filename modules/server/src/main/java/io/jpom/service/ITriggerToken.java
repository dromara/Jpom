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
