package io.jpom.model.user;

import io.jpom.model.BaseDbModel;
import io.jpom.model.data.UserModel;
import io.jpom.service.ITriggerToken;
import io.jpom.service.h2db.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * id 为 triggerToken
 *
 * @author bwcx_jzy
 * @since 2022/7/22
 */
@TableName(value = "TRIGGER_TOKEN_LOG", name = "触发器 token")
@Data
@EqualsAndHashCode(callSuper = true)
public class TriggerTokenLogBean extends BaseDbModel {
    /**
     * 为了兼容旧数据（因为旧数据字段长度大于 50 ）
     * <p>
     * 198fc5b944a3978b839506eb9d534c6f2b200dcda4e1d378fe30d2e8dbd7335bf4a
     */
    private String triggerToken;
    /**
     * 类型
     *
     * @see ITriggerToken#typeName()
     */
    private String type;

    /**
     * 关联数据ID
     */
    private String dataId;

    /**
     * 用户ID
     *
     * @see UserModel#getId()
     */
    private String userId;
}
