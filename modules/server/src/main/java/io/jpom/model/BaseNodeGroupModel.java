package io.jpom.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author bwcx_jzy
 * @since 2023/2/8
 */
@EqualsAndHashCode(callSuper = true)
@Data
public abstract class BaseNodeGroupModel extends BaseNodeModel {

    /**
     * 分组
     */
    private String group;
}
