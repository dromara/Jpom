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
@TableName(value = "STATIC_FILE_STORAGE", name = "静态文件管理")
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
