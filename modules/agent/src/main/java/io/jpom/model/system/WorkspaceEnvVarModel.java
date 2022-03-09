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
package io.jpom.model.system;

import io.jpom.model.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashMap;
import java.util.Map;

/**
 * @author lidaofu
 * @since 2022/3/8
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class WorkspaceEnvVarModel extends BaseModel {

    private Map<String, WorkspaceEnvVarItemModel> varData;

    /**
     * 更新变量
     *
     * @param name                 变量名称
     * @param workspaceEnvVarModel 变量信息
     */
    public void put(String name, WorkspaceEnvVarItemModel workspaceEnvVarModel) {
        if (varData == null) {
            varData = new HashMap<>(2);
        }
        varData.put(name, workspaceEnvVarModel);
    }

    /**
     * 删除 变量
     *
     * @param name 名称
     */
    public void remove(String name) {
        if (varData == null) {
            return;
        }
        varData.remove(name);
    }

    /**
     * @author lidaofu
     * @since 2022/3/8
     */
    @Data
    public static class WorkspaceEnvVarItemModel {

        private String name;

        private String value;

        private String description;
    }
}
