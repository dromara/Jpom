package org.dromara.jpom.func.assets.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.jpom.db.TableName;
import org.dromara.jpom.model.BaseUserModifyDbModel;

/**
 * @author bwcx_jzy1
 * @since 2024/6/1
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "SCRIPT_LIBRARY", name = "脚本库信息")
@Data
public class ScriptLibraryModel extends BaseUserModifyDbModel {
    /**
     * 脚本唯一的标签
     */
    private String tag;

    private String script;

    private String description;
    /**
     * 版本
     */
    private String version;
    /**
     * 关联的资产机器节点
     */
    private String machineIds;
}
