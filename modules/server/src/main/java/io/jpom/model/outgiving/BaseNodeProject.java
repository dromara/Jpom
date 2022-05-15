package io.jpom.model.outgiving;

import io.jpom.model.BaseJsonModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author bwcx_jzy
 * @since 2022/5/15
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class BaseNodeProject extends BaseJsonModel {

    private String nodeId;
    private String projectId;
}
