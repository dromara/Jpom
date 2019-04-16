package cn.keepbx.jpom.controller.node;

import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseController;
import cn.keepbx.jpom.model.data.NodeModel;
import cn.keepbx.jpom.service.node.NodeService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 节点管理
 *
 * @author jiangzeyin
 * @date 2019/4/16
 */
@RestController
@RequestMapping(value = "/node")
public class NodeEditController extends BaseController {

    @Resource
    private NodeService nodeService;

    @RequestMapping(value = "save.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String save(NodeModel model, String type) throws Exception {
        if ("add".equalsIgnoreCase(type)) {
            nodeService.addItem(model);
        } else {
            nodeService.updateItem(model);
        }
        return JsonMessage.getString(200, "操作成功");
    }
}
