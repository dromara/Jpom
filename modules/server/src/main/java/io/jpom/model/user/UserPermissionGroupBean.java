package io.jpom.model.user;

import io.jpom.model.BaseDbModel;
import io.jpom.model.BaseUserModifyDbModel;
import io.jpom.service.h2db.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author bwcx_jzy
 * @since 2022/8/3
 */
@TableName(value = "USER_PERMISSION_GROUP", name = "用户权限组")
@Data
@EqualsAndHashCode(callSuper = true)
public class UserPermissionGroupBean extends BaseUserModifyDbModel {

    /**
     * 名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 禁止执行时间段，优先判断禁止执行
     * <pre>
     * [{
     *     "startTime": 1,
     *     "endTime": 1,
     *     "reason": ""
     * }]
     * </pre>
     */
    private String prohibitExecute;

    /**
     * 允许执行的时间段
     * <pre>
     * [{
     *     "week": [1,2],
     *     "startTime":"08:00:00"
     *     "endTime": "18:00:00"
     * }]
     * </pre>
     */
    private String allowExecute;
}
