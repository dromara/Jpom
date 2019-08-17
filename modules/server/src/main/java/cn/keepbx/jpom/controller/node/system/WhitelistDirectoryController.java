package cn.keepbx.jpom.controller.node.system;

import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.common.forward.NodeForward;
import cn.keepbx.jpom.common.forward.NodeUrl;
import cn.keepbx.jpom.common.interceptor.OptLog;
import cn.keepbx.jpom.model.data.AgentWhitelist;
import cn.keepbx.jpom.model.log.UserOperateLogV1;
import cn.keepbx.jpom.service.system.WhitelistDirectoryService;
import cn.keepbx.permission.SystemPermission;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

/**
 * 白名单目录
 *
 * @author jiangzeyin
 * @date 2019/2/28
 */
@Controller
@RequestMapping(value = "/node/system")
public class WhitelistDirectoryController extends BaseServerController {
    @Resource
    private WhitelistDirectoryService whitelistDirectoryService;

    /**
     * 页面
     */
    @RequestMapping(value = "whitelistDirectory", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    @SystemPermission
    public String whitelistDirectory() {
        AgentWhitelist agentWhitelist = whitelistDirectoryService.getData(getNode());
        if (agentWhitelist != null) {
            setAttribute("project", AgentWhitelist.convertToLine(agentWhitelist.getProject()));
            //
            setAttribute("certificate", AgentWhitelist.convertToLine(agentWhitelist.getCertificate()));
            //
            setAttribute("nginx", AgentWhitelist.convertToLine(agentWhitelist.getNginx()));
        }
        return "node/system/whitelistDirectory";
    }

    /**
     * 保存接口
     *
     * @return json
     */
    @RequestMapping(value = "whitelistDirectory_submit", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @OptLog(UserOperateLogV1.OptType.EditWhitelist)
    @SystemPermission
    public String whitelistDirectorySubmit() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.WhitelistDirectory_Submit).toString();
    }
}
