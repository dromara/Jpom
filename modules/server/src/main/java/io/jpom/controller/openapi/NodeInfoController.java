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
package io.jpom.controller.openapi;

import cn.jiangzeyin.controller.base.AbstractController;
import io.jpom.common.ServerOpenApi;
import io.jpom.model.data.NodeModel;
import io.jpom.service.node.NodeService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 节点管理
 *
 * @author bwcx_jzy
 * @date 2019/8/5
 */
@RestController
public class NodeInfoController extends AbstractController {

    @Resource
    private NodeService nodeService;

    /**
     * 添加或者更新节点信息
     *
     * @param model 节点对象
     * @param type  操作类型
     * @return json
     */
    @RequestMapping(value = ServerOpenApi.UPDATE_NODE_INFO, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String update(NodeModel model, String type) {
        if ("add".equalsIgnoreCase(type)) {
            return nodeService.addNode(model, getRequest());
        } else {
            return nodeService.updateNode(model);
        }
    }
}
