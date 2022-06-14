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
package io.jpom.service.system;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import io.jpom.model.data.CertModel;
import io.jpom.service.BaseOperService;
import io.jpom.system.AgentConfigBean;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * @author Arno
 */
@Service
public class CertService extends BaseOperService<CertModel> {

    public CertService() {
        super(AgentConfigBean.CERT);
    }


    /**
     * 删除证书
     *
     * @param id id
     */
    @Override
    public void deleteItem(String id) {
        CertModel certModel = getItem(id);
        if (certModel == null) {
            return;
        }
        String keyPath = certModel.getCert();
        super.deleteItem(id);
        if (StrUtil.isNotEmpty(keyPath)) {
            // 删除证书文件
            File parentFile = FileUtil.file(keyPath).getParentFile();
            FileUtil.del(parentFile);
        }
    }
}
