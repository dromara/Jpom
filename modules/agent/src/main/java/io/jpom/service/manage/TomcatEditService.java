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
package io.jpom.service.manage;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONObject;
import io.jpom.model.data.TomcatInfoModel;
import io.jpom.service.BaseOperService;
import io.jpom.system.AgentConfigBean;
import org.springframework.stereotype.Service;

/**
 * @author lf
 */
@Service
public class TomcatEditService extends BaseOperService<TomcatInfoModel> {


    public TomcatEditService() {
        super(AgentConfigBean.TOMCAT);
    }


    /**
     * 根据tomcat名称查询tomcat信息
     *
     * @param name tomcat的名称
     * @return tomcat信息
     */
    public TomcatInfoModel getItemByName(String name) {
        JSONObject allTomcat = getJSONObject(AgentConfigBean.TOMCAT);

        if (allTomcat == null) {
            return null;
        }

        JSONObject tomcat = null;
        for (String key : allTomcat.keySet()) {
            JSONObject object = allTomcat.getJSONObject(key);
            if (name.equals(object.getString("name"))) {
                tomcat = object;
                break;
            }
        }

        return JSONObject.toJavaObject(tomcat, TomcatInfoModel.class);
    }

    /**
     * 添加Tomcat
     *
     * @param tomcatInfoModel tomcat信息
     */
    @Override
    public void addItem(TomcatInfoModel tomcatInfoModel) {
        tomcatInfoModel.setCreateTime(DateUtil.now());
        super.addItem(tomcatInfoModel);
    }


    /**
     * 修改tomcat信息
     *
     * @param tomcatInfoModel tomcat信息
     */
    @Override
    public void updateItem(TomcatInfoModel tomcatInfoModel) {
        tomcatInfoModel.setModifyTime(DateUtil.now());
        super.updateItem(tomcatInfoModel);

    }
}
