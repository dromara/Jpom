/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
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
    nameKey = "i18n.file_publish_template.745e")
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
