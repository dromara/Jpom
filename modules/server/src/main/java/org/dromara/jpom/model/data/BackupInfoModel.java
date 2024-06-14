/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.model.data;

import cn.hutool.core.annotation.PropIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.jpom.db.DbExtConfig;
import org.dromara.jpom.db.TableName;
import org.dromara.jpom.model.BaseUserModifyDbModel;

/**
 * @author Hotstrip
 * @since 2021-11-18
 * Backup info with H2 database
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "BACKUP_INFO",
    nameKey = "i18n.data_backup.9e26", modes = DbExtConfig.Mode.H2)
@Data
public class BackupInfoModel extends BaseUserModifyDbModel {

    /**
     * 备份名称
     */
    private String name;
    /**
     * 文件地址，绝对路径
     */
    private String filePath;
    /**
     * 备份类型{0: 全量, 1: 部分, 2: 导入, 3 自动}
     */
    private Integer backupType;
    /**
     * 文件大小
     */
    private Long fileSize;
    /**
     * SHA1SUM
     */
    private String sha1Sum;

    /**
     * 状态{0: 默认; 1: 成功; 2: 失败}
     */
    private Integer status;

    /**
     * 服务端版本
     */
    private String version;

    /**
     * 打包时间
     */
    private Long baleTimeStamp;
    /**
     * 文件是否存在
     */
    @PropIgnore
    private Boolean fileExist;

}
