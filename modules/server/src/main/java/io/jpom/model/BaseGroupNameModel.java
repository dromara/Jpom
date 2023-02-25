package io.jpom.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author bwcx_jzy
 * @since 2023/2/25
 */
@EqualsAndHashCode(callSuper = true)
@Data
public abstract class BaseGroupNameModel extends BaseUserModifyDbModel {
    /**
     * 名称
     */
    private String name;
    /**
     * 分组名称
     */
    private String groupName;
}
