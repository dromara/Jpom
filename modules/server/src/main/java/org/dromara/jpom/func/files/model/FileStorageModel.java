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

import cn.hutool.core.annotation.PropIgnore;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.SystemClock;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.jpom.db.TableName;
import org.dromara.jpom.model.BaseWorkspaceModel;

/**
 * @author bwcx_jzy
 * @since 2023/3/16
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "FILE_STORAGE",
    nameKey = "i18n.file_management_center.0f5f")
@Data
@NoArgsConstructor
public class FileStorageModel extends BaseWorkspaceModel implements IFileStorage {

    @Override
    public void setId(String id) {
        // 文件 md5
        super.setId(id);
    }

    /**
     * 文件名
     */
    private String name;

    public void setName(String name) {
        this.name = StrUtil.maxLength(name, 240);
    }

    /**
     * 文件大小
     */
    private Long size;
    /**
     * 文件描述
     */
    private String description;
    /**
     * 文件来源 0 上传 1 构建 2 下载 3 证书
     */
    private Integer source;
    /**
     * 文件有效期（毫秒）
     */
    private Long validUntil;
    /**
     * 文件路径
     */
    private String path;
    /**
     * 文件扩展名
     */
    private String extName;
    /**
     * 只有下载的时候才使用本字段
     * <p>
     * 0 下载中 1 下载完成 2 下载异常
     */
    private Integer status;
    /**
     * 进度描述
     */
    private String progressDesc;
    /**
     * 文件是否存在
     */
    @PropIgnore
    private Boolean exists;
    /**
     * 触发器 token
     */
    private String triggerToken;

    /**
     * 别名码
     */
    private String aliasCode;

    /**
     * 设置保留天数的过期时间
     *
     * @param keepDay   保留天数
     * @param startTime 文件开始的时间
     */
    public void validUntil(Integer keepDay, Long startTime) {
        int keepDayInt = ObjectUtil.defaultIfNull(keepDay, 3650);
        keepDayInt = Math.max(keepDayInt, 1);
        DateTime dateTime = new DateTime(ObjectUtil.defaultIfNull(startTime, SystemClock.now())).offset(DateField.DAY_OF_YEAR, keepDayInt);
        this.setValidUntil(DateUtil.endOfDay(dateTime).getTime());
    }

    @Override
    protected boolean hasCreateUser() {
        return true;
    }
}
