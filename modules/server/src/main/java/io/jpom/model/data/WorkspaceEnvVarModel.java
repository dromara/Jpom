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
package io.jpom.model.data;

import io.jpom.model.BaseWorkspaceModel;
import io.jpom.service.h2db.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工作空间环境变量
 *
 * @author bwcx_jzy
 * @since 2021/12/10
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "WORKSPACE_ENV_VAR", name = "工作空间环境变量")
@Data
public class WorkspaceEnvVarModel extends BaseWorkspaceModel {

    /**
     * 名称
     */
    private String name;
    /**
     * 值
     */
    private String value;
    /**
     * 描述
     */
    private String description;
    /**
     * 节点ID
     */
    private String nodeIds;
    /**
     * 隐私变量{1，隐私变量，0 非隐私变量（明文回显）}
     */
    private Integer privacy;
}
