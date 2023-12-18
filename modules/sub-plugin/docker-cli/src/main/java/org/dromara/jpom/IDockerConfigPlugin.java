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
