/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 文件管理存储
 *
 * @author bwcx_jzy
 * @since 23/12/25 025
 */
@Data
@ConfigurationProperties("jpom.file-storage")
public class FileStorageConfig {

    /**
     * 文件中心存储路径
     */
    private String savePah;
    /**
     * 静态目录扫描周期
     * <p>
     * 0 0/1 * * *
     */
    private String scanStaticDirCron = "0 0/1 * * *";
    /**
     * 开启静态目录监听
     */
    private Boolean watchMonitorStaticDir = true;
    /**
     * 监听深度
     */
    private Integer watchMonitorMaxDepth = 1;
}
