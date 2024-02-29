/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author lf
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AgentFileModel extends BaseModel {

    /**
     * 保存Agent文件
     */
    public static final String ID = "AGENT_FILE";
    /**
     * 最新插件端包的文件名
     */
    public static final String ZIP_NAME = "agent.zip";
    /**
     * 默认空版本信息
     */
    public static final AgentFileModel EMPTY = new AgentFileModel();
    /**
     * 文件大小
     */
    private Long size;
    /**
     * 保存路径
     */
    private String savePath;
    /**
     * 版本号
     */
    private String version;
    /**
     * jar 打包时间
     */
    private String timeStamp;

    @Override
    public String toString() {
        return super.toString();
    }
}
