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
package io.jpom.controller.script;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HtmlUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.controller.multipart.MultipartFileBuilder;
import io.jpom.JpomApplication;
import io.jpom.common.BaseAgentController;
import io.jpom.model.data.ScriptModel;
import io.jpom.service.script.ScriptServer;
import io.jpom.system.AgentConfigBean;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;

/**
 * 脚本管理
 *
 * @author jiangzeyin
 * @date 2019/4/24
 */
@RestController
@RequestMapping(value = "/script")
public class ScriptController extends BaseAgentController {
    @Resource
    private ScriptServer scriptServer;

    @RequestMapping(value = "list.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String list() {
        return JsonMessage.getString(200, "", scriptServer.list());
    }

    @RequestMapping(value = "item.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String item(String id) {
        return JsonMessage.getString(200, "", scriptServer.getItem(id));
    }

    @RequestMapping(value = "save.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String save(ScriptModel scriptModel, String type) {
        if (scriptModel == null) {
            return JsonMessage.getString(405, "没有数据");
        }
        if (StrUtil.isEmpty(scriptModel.getContext())) {
            return JsonMessage.getString(405, "内容为空");
        }
        //
        scriptModel.setContext(HtmlUtil.unescape(scriptModel.getContext()));
        ScriptModel eModel = scriptServer.getItem(scriptModel.getId());
        if ("add".equalsIgnoreCase(type)) {
            if (eModel != null) {
                return JsonMessage.getString(405, "id已经存在啦");
            }
            scriptModel.setId(IdUtil.fastSimpleUUID());
            File file = scriptModel.getFile(true);
            if (file.exists() || file.isDirectory()) {
                return JsonMessage.getString(405, "当地id路径文件已经存在来，请修改");
            }
            scriptServer.addItem(scriptModel);
            return JsonMessage.getString(200, "添加成功");
        }
        if (eModel == null) {
            return JsonMessage.getString(405, "对应数据不存在");
        }
        eModel.setName(scriptModel.getName());
        eModel.setContext(scriptModel.getContext());
        scriptServer.updateItem(eModel);
        return JsonMessage.getString(200, "修改成功");
    }

    @RequestMapping(value = "del.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String del(String id) {
        scriptServer.deleteItem(id);
        return JsonMessage.getString(200, "删除成功");
    }

    @RequestMapping(value = "upload.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String upload() throws IOException {
        MultipartFileBuilder multipartFileBuilder = createMultipart()
                .addFieldName("file").setFileExt("bat", "sh");
        multipartFileBuilder.setSavePath(AgentConfigBean.getInstance().getTempPathName());
        multipartFileBuilder.setUseOriginalFilename(true);
        String path = multipartFileBuilder.save();
        File file = FileUtil.file(path);
        String context = FileUtil.readString(path, JpomApplication.getCharset());
        if (StrUtil.isEmpty(context)) {
            return JsonMessage.getString(405, "脚本内容为空");
        }
        String id = file.getName();
        ScriptModel eModel = scriptServer.getItem(id);
        if (eModel != null) {
            return JsonMessage.getString(405, "对应脚本模板已经存在啦");
        }
        eModel = new ScriptModel();
        eModel.setId(id);
        eModel.setName(id);
        eModel.setContext(context);
        file = eModel.getFile(true);
        if (file.exists() || file.isDirectory()) {
            return JsonMessage.getString(405, "当地id路径文件已经存在来，请修改");
        }
        scriptServer.addItem(eModel);
        return JsonMessage.getString(200, "导入成功");
    }
}
