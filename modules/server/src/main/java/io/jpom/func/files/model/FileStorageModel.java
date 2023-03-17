/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.jpom.func.files.model;

import cn.hutool.core.annotation.PropIgnore;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.SystemClock;
import cn.hutool.core.util.ObjectUtil;
import io.jpom.model.BaseWorkspaceModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import top.jpom.h2db.TableName;

/**
 * @author bwcx_jzy
 * @since 2023/3/16
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "FILE_STORAGE", name = "文件管理中心")
@Data
@NoArgsConstructor
public class FileStorageModel extends BaseWorkspaceModel {

    @Override
    public void setId(String id) {
        // 文件 md5
        super.setId(id);
    }

    /**
     * 文件名
     */
    private String name;
    /**
     * 文件大小
     */
    private Long size;
    /**
     * 文件描述
     */
    private String description;
    /**
     * 文件来源 0 上传 1 构建 2 下载
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
