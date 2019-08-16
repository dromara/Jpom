package cn.keepbx.jpom.controller.node;

import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.common.forward.NodeForward;
import cn.keepbx.jpom.common.forward.NodeUrl;
import cn.keepbx.jpom.common.interceptor.OptLog;
import cn.keepbx.jpom.model.log.UserOperateLogV1;
import cn.keepbx.jpom.service.system.WhitelistDirectoryService;
import cn.keepbx.plugin.ClassFeature;
import cn.keepbx.plugin.Feature;
import cn.keepbx.plugin.MethodFeature;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 节点初始化
 *
 * @author jiangzeyin
 * @date 2019/4/17
 */
@RestController
@RequestMapping(value = "/node")
@Feature(cls = ClassFeature.NODE)
public class NodeInstallController extends BaseServerController {

    @Resource
    private WhitelistDirectoryService whitelistDirectoryService;

    @RequestMapping(value = "install_node.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @OptLog(UserOperateLogV1.OptType.InstallNode)
    @Feature(method = MethodFeature.INSTALL)
    public String installNode() {
        List<String> list = whitelistDirectoryService.getProjectDirectory(getNode());
        if (list != null && !list.isEmpty()) {
            return JsonMessage.getString(402, "节点已经初始化过啦");
        }
        return NodeForward.request(getNode(), getRequest(), NodeUrl.WhitelistDirectory_Submit).toString();
    }
}
