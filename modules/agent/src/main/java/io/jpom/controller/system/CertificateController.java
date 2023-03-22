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
package io.jpom.controller.system;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.extra.servlet.ServletUtil;
import io.jpom.common.BaseAgentController;
import io.jpom.common.JsonMessage;
import io.jpom.common.validator.ValidatorItem;
import io.jpom.model.data.CertModel;
import io.jpom.service.system.CertService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.List;

/**
 * 证书管理
 *
 * @author Arno
 */
@RestController
@RequestMapping(value = "/system/certificate")
@Slf4j
public class CertificateController extends BaseAgentController {

    private final CertService certService;


    public CertificateController(CertService certService) {
        this.certService = certService;
    }


    /**
     * 证书列表
     *
     * @return json
     */
    @RequestMapping(value = "/getCertList", produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonMessage<List<CertModel>> getCertList() {
        List<CertModel> array = certService.list();
        return JsonMessage.success("", array);
    }

    /**
     * 删除证书
     *
     * @param id id
     * @return json
     */
    @RequestMapping(value = "/delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonMessage<String> delete(@ValidatorItem String id) {
        certService.deleteItem(id);
        return JsonMessage.success("删除成功");
    }


    /**
     * 导出证书
     *
     * @param id 项目id
     */
    @RequestMapping(value = "/export", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public void export(String id, HttpServletResponse response) {
        CertModel item = certService.getItem(id);
        Assert.notNull(item, "导出失败");
        String parent = FileUtil.file(item.getCert()).getParent();
        File zip = ZipUtil.zip(parent);
        ServletUtil.write(response, zip);
        FileUtil.del(zip);
    }
}
