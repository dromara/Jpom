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

import cn.jiangzeyin.common.DefaultSystemLog;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseDataService;
import io.jpom.model.data.ServerWhitelist;
import io.jpom.system.ServerConfigBean;
import io.jpom.util.JsonFileUtil;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author jiangzeyin
 * @date 2019/4/22
 */
@Service
public class ServerWhitelistServer extends BaseDataService {

    public ServerWhitelist getWhitelist() {
        try {
            JSONObject jsonObject = getJSONObject(ServerConfigBean.OUTGIVING_WHITELIST);
            if (jsonObject == null) {
                return new ServerWhitelist();
            }
            return jsonObject.toJavaObject(ServerWhitelist.class);
        } catch (Exception e) {
            DefaultSystemLog.getLog().error(e.getMessage(), e);
        }
        return new ServerWhitelist();
    }

    public void saveWhitelistDirectory(ServerWhitelist serverWhitelist) {
        String path = getDataFilePath(ServerConfigBean.OUTGIVING_WHITELIST);
        JsonFileUtil.saveJson(path, serverWhitelist.toJson());
    }


    public List<String> getOutGiving() {
        ServerWhitelist serverWhitelist = getWhitelist();
        if (serverWhitelist == null) {
            return null;
        }
        return serverWhitelist.getOutGiving();
    }
}
