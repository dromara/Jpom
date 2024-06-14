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

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.jpom.db.TableName;
import org.dromara.jpom.model.BaseUserModifyDbModel;

/**
 * @author bwcx_jzy
 * @since 23/12/28 028
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "STATIC_FILE_STORAGE",
    nameKey = "i18n.static_file_management.6ac2")
@Data
@NoArgsConstructor
public class StaticFileStorageModel extends BaseUserModifyDbModel implements IFileStorage {

    /**
     * 文件名
     */
    private String name;

    /**
     * 只保留 100 字符
     *
     * @param name 名称
     */
    public void setName(String name) {
        this.name = StrUtil.maxLength(name, 100);
    }

    /**
     * 文件大小
     */
    private Long size;
    /**
     * 文件路径
     */
    private String absolutePath;
    private String parentAbsolutePath;
    /**
     * 要组索引不字段不能太长
     * [42000][1071] Specified key was too long; max key length is 3072 bytes
     */
    private String staticDir;
    private Integer level;
    /**
     * 文件修改时间
     */
    private Long lastModified;
    /**
     * 文件扩展名
     */
    private String extName;
    /**
     * 文件状态
     * 0 不存在
     * 1 存在
     */
    private Integer status;
    /**
     * 文件类型
     * 0 文件夹
     * 1 文件
     */
    private Integer type;
    /**
     * 扫描任务id
     */
    private Long scanTaskId;
    /**
     * 描述
     */
    private String description;
    /**
     * 触发器 token
     */
    private String triggerToken;

    @Override
    protected boolean hasCreateUser() {
        return false;
    }

    public int type() {
        return ObjectUtil.defaultIfNull(this.getType(), 0);
    }
}
