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
import io.jpom.model.data.ProjectRecoverModel;
import io.jpom.service.BaseOperService;
import io.jpom.system.AgentConfigBean;
import org.springframework.stereotype.Service;

/**
 * 项目管理
 *
 * @author jiangzeyin
 */
@Service
public class ProjectRecoverService extends BaseOperService<ProjectRecoverModel> {

    public ProjectRecoverService() {
        super(AgentConfigBean.PROJECT_RECOVER);
    }


    /**
     * 保存项目信息
     *
     * @param projectInfo 项目
     */
    @Override
    public void addItem(ProjectRecoverModel projectInfo) {
        projectInfo.setDelTime(DateUtil.now());
        // 保存
        super.addItem(projectInfo);
    }
}
