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

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import io.jpom.common.BaseAgentController;
import io.jpom.model.data.JdkInfoModel;
import io.jpom.service.manage.JdkInfoService;
import io.jpom.util.FileUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.File;
import java.util.List;

/**
 * @author bwcx_jzy
 * @since 2019/11/1
 */
@RestController
@RequestMapping(value = "/manage/jdk/")
public class JdkListController extends BaseAgentController {

    @Resource
    private JdkInfoService jdkInfoService;

    @RequestMapping(value = "list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String list() {
        List<JdkInfoModel> list = jdkInfoService.list();
        return JsonMessage.getString(200, "", list);
    }

    @RequestMapping(value = "update", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String update(JdkInfoModel model) {
        String path = model.getPath();
        if (StrUtil.isEmpty(path)) {
            return JsonMessage.getString(400, "请填写jdk路径");
        }
        String newPath = FileUtil.normalize(path);
        File file = FileUtil.file(newPath);
        //去除bin
        if ("bin".equals(file.getName())) {
            newPath = file.getParentFile().getAbsolutePath();
        }
        if (!FileUtils.isJdkPath(newPath)) {
            return JsonMessage.getString(400, "路径错误，该路径不是jdk路径");
        }
        model.setPath(newPath);
        String id = model.getId();
        String jdkVersion = FileUtils.getJdkVersion(newPath);
        model.setVersion(jdkVersion);
        String name = model.getName();
        if (StrUtil.isEmpty(name)) {
            model.setName(jdkVersion);
        }
        if (StrUtil.isEmpty(id)) {
            model.setId(IdUtil.fastSimpleUUID());
            jdkInfoService.addItem(model);
            return JsonMessage.getString(200, "添加成功");
        }
        jdkInfoService.updateItem(model);
        return JsonMessage.getString(200, "修改成功");
    }

    @RequestMapping(value = "delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String delete(String id) {
        if (StrUtil.isEmpty(id)) {
            return JsonMessage.getString(400, "删除失败");
        }
        jdkInfoService.deleteItem(id);
        return JsonMessage.getString(200, "删除成功");
    }
}
