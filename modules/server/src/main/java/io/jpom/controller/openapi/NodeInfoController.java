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
    @RequestMapping(value = ServerOpenApi.UPDATE_NODE_INFO, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String update(NodeModel model, String type) {
        if ("add".equalsIgnoreCase(type)) {
            return nodeService.addNode(model, getRequest());
        } else {
            return nodeService.updateNode(model);
        }
    }
}
