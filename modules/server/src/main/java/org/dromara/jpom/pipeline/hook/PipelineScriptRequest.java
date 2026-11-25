/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.pipeline.hook;

import lombok.Data;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 一次脚本执行的请求
 *
 * @author jpom
 * @since 2.11.16
 */
@Data
public class PipelineScriptRequest {

    /**
     * 服务端脚本 ID
     */
    private String scriptId;
    /**
     * 工作空间（执行记录的归属）
     */
    private String workspaceId;
    /**
     * 执行目录
     */
    private File workDir;
    /**
     * 环境变量
     */
    private Map<String, String> env = new LinkedHashMap<>();
}
