package org.dromara.jpom.func.files.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.jpom.db.TableName;
import org.dromara.jpom.model.BaseWorkspaceModel;

/**
 * @author bwcx_jzy
 * @since 2025/1/9
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "FILE_RELEASE_TASK_TEMPLATE",
    nameKey = "文件发布模板")
@Data
public class FileReleaseTaskTemplate extends BaseWorkspaceModel {

    /**
     * 模板名称
     */
    private String name;
    /**
     * 模板标签
     */
    private String templateTag;
    /**
     * 模板数据
     */
    private String data;

    private Integer fileType;
}
