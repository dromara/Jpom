/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom;

import cn.hutool.core.io.FileUtil;
import org.dromara.jpom.plugin.IDefaultPlugin;

import java.io.File;
import java.io.InputStream;

/**
 * @author bwcx_jzy
 * @since 2023/1/4
 */
public interface IDockerConfigPlugin extends IDefaultPlugin {


    /**
     * 获取 配置资源
     *
     * @param name 配置文件名称
     * @return 文件流
     */
    @Override
    default InputStream getConfigResourceInputStream(String name) {
        String newName = "docker/" + name;
        return IDefaultPlugin.super.getConfigResourceInputStream(newName);
    }

    /**
     * 获取配置文件
     *
     * @param name    配置文件名称
     * @param tempDir 保存目录
     * @return file
     */
    default File getResourceToFile(String name, File tempDir) {
        InputStream stream = this.getConfigResourceInputStream(name);
        if (stream == null) {
            return null;
        }
        File tempFile = DockerUtil.createTemp(name, tempDir);
        FileUtil.writeFromStream(stream, tempFile);
        return tempFile;
    }
}
