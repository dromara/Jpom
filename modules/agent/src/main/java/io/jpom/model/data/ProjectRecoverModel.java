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
package io.jpom.model.data;

import cn.hutool.core.lang.ObjectId;
import io.jpom.model.BaseModel;

/**
 * 项目回收记录实体
 *
 * @author jiangzeyin
 */
public class ProjectRecoverModel extends BaseModel {
    /**
     * 删除人
     */
    private String delUser;
    /**
     * 删除时间
     */
    private String delTime;
    /**
     * 删除的对应项目信息
     */
    private NodeProjectInfoModel nodeProjectInfoModel;

    public ProjectRecoverModel(NodeProjectInfoModel nodeProjectInfoModel) {
        this.nodeProjectInfoModel = nodeProjectInfoModel;
        // 生成操作id
        setId(ObjectId.next());
    }

    public ProjectRecoverModel() {
    }

    public NodeProjectInfoModel getProjectInfoModel() {
        return nodeProjectInfoModel;
    }

    public void setProjectInfoModel(NodeProjectInfoModel nodeProjectInfoModel) {
        this.nodeProjectInfoModel = nodeProjectInfoModel;
    }

    public String getDelUser() {
        return delUser;
    }

    public void setDelUser(String delUser) {
        this.delUser = delUser;
    }

    public String getDelTime() {
        return delTime;
    }

    public void setDelTime(String delTime) {
        this.delTime = delTime;
    }
}
