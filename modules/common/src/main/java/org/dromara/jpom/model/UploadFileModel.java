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

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author lf
 */
@EqualsAndHashCode(callSuper = true)
@Slf4j
@Data
public class UploadFileModel extends BaseModel {
    private long size = 0;
    private long completeSize = 0;
    private String savePath;
    private String version;

    public void save(byte[] data) {
        this.completeSize += data.length;
        File file = new File(this.getFilePath());
        FileUtil.mkParentDirs(file);
        try (FileOutputStream fileOutputStream = new FileOutputStream(file, true)) {
            fileOutputStream.write(data);
            fileOutputStream.flush();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public String getFilePath() {
        return savePath + StrUtil.SLASH + getName();
    }

    public void remove() {
        FileUtil.del(this.getFilePath());
    }
}
