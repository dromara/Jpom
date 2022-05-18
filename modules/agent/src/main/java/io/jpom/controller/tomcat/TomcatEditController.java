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
package io.jpom.controller.tomcat;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.crypto.SecureUtil;
import cn.jiangzeyin.common.JsonMessage;
import io.jpom.common.BaseAgentController;
import io.jpom.model.data.TomcatInfoModel;
import io.jpom.service.manage.TomcatEditService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

/**
 * tomcat 编辑
 *
 * @author bwcx_jzy
 * @since 2019/7/21
 */
@RestController
@RequestMapping(value = "/tomcat/")
public class TomcatEditController extends BaseAgentController {
    @Resource
    private TomcatEditService tomcatEditService;


    /**
     * 列出所有的tomcat
     *
     * @return Tomcat列表
     */
    @RequestMapping(value = "list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String list() {
        // 查询tomcat列表
        List<TomcatInfoModel> tomcatInfoModels = tomcatEditService.list();
        return JsonMessage.getString(200, "查询成功", tomcatInfoModels);
    }

    /**
     * 根据Id查询Tomcat信息
     *
     * @param id Tomcat的主键
     * @return 操作结果
     */
    @RequestMapping(value = "getItem", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String getItem(String id) {
        // 查询tomcat列表
        return JsonMessage.getString(200, "查询成功", tomcatEditService.getItem(id));
    }


    /**
     * 添加Tomcat
     *
     * @param tomcatInfoModel Tomcat信息
     * @return 操作结果
     */
    @RequestMapping(value = "add", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String add(TomcatInfoModel tomcatInfoModel) {
        // 根据Tomcat名称查询tomcat是否已经存在
        String name = tomcatInfoModel.getName();
        TomcatInfoModel tomcatInfoModelTemp = tomcatEditService.getItemByName(name);
        if (tomcatInfoModelTemp != null) {
            return JsonMessage.getString(401, "名称已经存在，请使用其他名称！");
        }
        tomcatInfoModel.setId(SecureUtil.md5(DateUtil.now()));
        tomcatInfoModel.setCreator(getUserName());

        // 设置tomcat路径，去除多余的符号
        tomcatInfoModel.setPath(FileUtil.normalize(tomcatInfoModel.getPath()));
        Objects.requireNonNull(tomcatInfoModel.pathAndCheck());
        tomcatEditService.addItem(tomcatInfoModel);
        tomcatInfoModel.initTomcat();
        return JsonMessage.getString(200, "保存成功");
    }


    /**
     * 修改Tomcat信息
     *
     * @param tomcatInfoModel Tomcat信息
     * @return 操作结果
     */
    @RequestMapping(value = "update", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String update(TomcatInfoModel tomcatInfoModel) {
        // 根据Tomcat名称查询tomcat是否已经存在
        String name = tomcatInfoModel.getName();
        TomcatInfoModel tomcatInfoModelTemp = tomcatEditService.getItemByName(name);
        if (tomcatInfoModelTemp != null && !tomcatInfoModelTemp.getId().equals(tomcatInfoModel.getId())) {
            return JsonMessage.getString(401, "名称已经存在，请使用其他名称！");
        }

        tomcatInfoModel.setModifyUser(getUserName());
        // 设置tomcat路径，去除多余的符号
        tomcatInfoModel.setPath(FileUtil.normalize(tomcatInfoModel.getPath()));
        Objects.requireNonNull(tomcatInfoModel.pathAndCheck());
        tomcatEditService.updateItem(tomcatInfoModel);
        tomcatInfoModel.initTomcat();
        return JsonMessage.getString(200, "修改成功");

    }


    /**
     * 删除tomcat
     *
     * @param id tomcat id
     * @return 操作结果
     */
    @RequestMapping(value = "delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String delete(String id) {
        tomcatEditService.deleteItem(id);
        return JsonMessage.getString(200, "删除成功");
    }
}
