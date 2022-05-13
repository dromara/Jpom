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
package io.jpom.controller.manage;

import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import io.jpom.common.BaseJpomController;
import io.jpom.model.data.ProjectRecoverModel;
import io.jpom.service.manage.ProjectRecoverService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;

/**
 * 回收站管理
 *
 * @author jiangzeyin
 * @since 2019/4/16
 */
@RestController
@RequestMapping(value = "/manage/recover")
public class ProjectRecoverControl extends BaseJpomController {
    @Resource
    private ProjectRecoverService projectRecoverService;

    @RequestMapping(value = "list_data", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String projectInfo() {
        List<ProjectRecoverModel> projectInfoModels = projectRecoverService.list();
        return JsonMessage.getString(200, "", projectInfoModels);
    }

    @RequestMapping(value = "item_data", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String project(String id) throws IOException {
        if (StrUtil.isEmpty(id)) {
            return JsonMessage.getString(400, "项目id错误");
        }
        ProjectRecoverModel item = projectRecoverService.getItem(id);
        return JsonMessage.getString(200, "", item);
    }
}
