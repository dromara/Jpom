package io.jpom.controller.node.system;

import cn.jiangzeyin.common.JsonMessage;
import io.jpom.common.BaseServerController;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.common.interceptor.OptLog;
import io.jpom.model.data.AgentWhitelist;
import io.jpom.model.log.UserOperateLogV1;
import io.jpom.permission.SystemPermission;
import io.jpom.service.system.WhitelistDirectoryService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

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

//    /**
//     * 页面
//     */
//    @RequestMapping(value = "whitelistDirectory", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
//    @SystemPermission
//    public String whitelistDirectory() {
//        AgentWhitelist agentWhitelist = whitelistDirectoryService.getData(getNode());
//        if (agentWhitelist != null) {
//            setAttribute("project", AgentWhitelist.convertToLine(agentWhitelist.getProject()));
//            //
//            setAttribute("certificate", AgentWhitelist.convertToLine(agentWhitelist.getCertificate()));
//            //
//            setAttribute("nginx", AgentWhitelist.convertToLine(agentWhitelist.getNginx()));
//        }
//        return "node/system/whitelistDirectory";
//    }


    /**
     * @author Hotstrip
     * get whiteList data
     * 白名单数据接口
     * @return
     */
    @RequestMapping(value = "white-list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @SystemPermission
    @ResponseBody
    public String whiteList() {
        AgentWhitelist agentWhitelist = whitelistDirectoryService.getData(getNode());
        Map<String, String> map = new HashMap();
        if (agentWhitelist != null) {
            /**
             * put key and value into map
             * 赋值给 map 对象返回
             */
            map.put("project", AgentWhitelist.convertToLine(agentWhitelist.getProject()));
            map.put("certificate", AgentWhitelist.convertToLine(agentWhitelist.getCertificate()));
            map.put("nginx", AgentWhitelist.convertToLine(agentWhitelist.getNginx()));
        }
        return JsonMessage.getString(200, "ok", map);
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
