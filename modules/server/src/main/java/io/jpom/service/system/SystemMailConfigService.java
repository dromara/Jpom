/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 码之科技工作室
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

import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseDataService;
import io.jpom.model.data.MailAccountModel;
import io.jpom.system.ServerConfigBean;
import io.jpom.util.JsonFileUtil;
import org.springframework.stereotype.Service;

/**
 * 监控管理Service
 *
 * @author Arno
 */
@Service
public class SystemMailConfigService extends BaseDataService {

    /**
     * 获取配置
     *
     * @return config
     */
    public MailAccountModel getConfig() {
        JSONObject config = getJSONObject(ServerConfigBean.MAIL_CONFIG);
        if (config == null) {
            return null;
        }
        return config.toJavaObject(MailAccountModel.class);
    }

    /**
     * 保存配置
     *
     * @param mailAccountModel config
     */
    public void save(MailAccountModel mailAccountModel) {
        String path = getDataFilePath(ServerConfigBean.MAIL_CONFIG);
        JsonFileUtil.saveJson(path, mailAccountModel.toJson());
    }
}
