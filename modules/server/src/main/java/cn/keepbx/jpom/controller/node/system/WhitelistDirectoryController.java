package cn.keepbx.jpom.controller.node.system;

import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.common.Role;
import cn.keepbx.jpom.common.forward.NodeForward;
import cn.keepbx.jpom.common.forward.NodeUrl;
import cn.keepbx.jpom.common.interceptor.UrlPermission;
import cn.keepbx.jpom.model.data.Whitelist;
import cn.keepbx.jpom.service.system.WhitelistDirectoryService;
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
    public String whitelistDirectory() {
        Whitelist whitelist = whitelistDirectoryService.getData(getNode());
        if (whitelist != null) {
            setAttribute("project", whitelistDirectoryService.convertToLine(whitelist.getProject()));
            //
            setAttribute("certificate", whitelistDirectoryService.convertToLine(whitelist.getCertificate()));
            //
            setAttribute("nginx", whitelistDirectoryService.convertToLine(whitelist.getNginx()));
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
    @UrlPermission(Role.System)
    public String whitelistDirectorySubmit() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.WhitelistDirectory_Submit).toString();
    }
}
