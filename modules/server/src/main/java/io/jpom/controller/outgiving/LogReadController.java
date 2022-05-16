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
package io.jpom.controller.outgiving;

import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.model.PageResultDto;
import io.jpom.model.outgiving.LogReadModel;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.service.outgiving.LogReadServer;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 日志阅读
 *
 * @author bwcx_jzy
 * @since 2022/5/15
 */
@RestController
@RequestMapping(value = "/log-read")
@Feature(cls = ClassFeature.LOG_READ)
public class LogReadController extends BaseServerController {

    private final LogReadServer logReadServer;

    public LogReadController(LogReadServer logReadServer) {
        this.logReadServer = logReadServer;
    }

    /**
     * 日志阅读列表
     *
     * @return json
     */
    @PostMapping(value = "list", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public String list() {
        PageResultDto<LogReadModel> pageResultDto = logReadServer.listPage(getRequest());
        return JsonMessage.getString(200, "success", pageResultDto);
    }

    /**
     * 删除日志阅读信息
     *
     * @param id 分发id
     * @return json
     */
    @RequestMapping(value = "del.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public String del(String id) {
        HttpServletRequest request = getRequest();
        int byKey = logReadServer.delByKey(id, request);
        return JsonMessage.getString(200, "操作成功");
    }

    /**
     * 编辑日志阅读信息
     * <p>
     * {"projectList":[{"nodeId":"localhost","projectId":"test-jar"}],"name":"11"}
     *
     * @param jsonObject 参数
     * @return msg
     */
    @RequestMapping(value = "save.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public String save(@RequestBody JSONObject jsonObject) {
        Assert.notNull(jsonObject, "请传入参数");
        String id = jsonObject.getString("id");
        String name = jsonObject.getString("name");
        Assert.hasText(name, "请填写名称");
        JSONArray projectListArray = jsonObject.getJSONArray("projectList");
        Assert.notEmpty(projectListArray, "请选择节点和项目");
        List<LogReadModel.Item> projectList = projectListArray.toJavaList(LogReadModel.Item.class);
        projectList = projectList.stream()
            .filter(item -> StrUtil.isAllNotEmpty(item.getNodeId(), item.getProjectId()))
            .collect(Collectors.toList());
        Assert.notEmpty(projectList, "请选择节点和项目");
        LogReadModel logReadModel = new LogReadModel();
        logReadModel.setId(id);
        logReadModel.setName(name);
        logReadModel.setNodeProject(JSONArray.toJSONString(projectList));
        //
        if (StrUtil.isEmpty(id)) {
            logReadServer.insert(logReadModel);
        } else {
            HttpServletRequest request = getRequest();
            logReadServer.updateById(logReadModel, request);
        }
        return JsonMessage.getString(200, "修改成功");
    }

    /**
     * 更新缓存
     * <p>
     * {"op":"showlog","projectId":"python",
     * "search":true,"useProjectId":"python",
     * "useNodeId":"localhost",
     * "beforeCount":0,"afterCount":10,
     * "head":0,"tail":100,"first":"false",
     * "logFile":"/run.log"}
     *
     * @param jsonObject 参数
     * @return msg
     */
    @RequestMapping(value = "update-cache.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public String updateCache(@RequestBody JSONObject jsonObject) {
        Assert.notNull(jsonObject, "请传入参数");
        String id = jsonObject.getString("id");
        Assert.hasText(id, "请传入参数");
        LogReadModel.CacheDta cacheDta = jsonObject.toJavaObject(LogReadModel.CacheDta.class);
        HttpServletRequest request = getRequest();
        LogReadModel logReadModel = new LogReadModel();
        logReadModel.setId(id);
        logReadModel.setCacheData(JSONArray.toJSONString(cacheDta));
        logReadServer.updateById(logReadModel, request);
        return JsonMessage.getString(200, "修改成功");
    }
}
