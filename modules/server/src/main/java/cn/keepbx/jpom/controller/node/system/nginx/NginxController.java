package cn.keepbx.jpom.controller.node.system.nginx;

import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.common.forward.NodeForward;
import cn.keepbx.jpom.common.forward.NodeUrl;
import cn.keepbx.jpom.common.interceptor.UrlPermission;
import cn.keepbx.jpom.model.Role;
import cn.keepbx.jpom.model.data.UserModel;
import cn.keepbx.jpom.model.log.UserOperateLogV1;
import cn.keepbx.jpom.service.system.WhitelistDirectoryService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.List;

/**
 * nginx 管理
 *
 * @author Arno
 */
@Controller
@RequestMapping("/node/system/nginx")
public class NginxController extends BaseServerController {

    @Resource
    private WhitelistDirectoryService whitelistDirectoryService;


    @RequestMapping(value = "list.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String ngx() {
        return "node/system/nginx";
    }

    /**
     * 配置列表
     */
    @RequestMapping(value = "list_data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    //    @UrlPermission(Role.NodeManage)
    public String list() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.System_Nginx_list_data).toString();
    }

    @RequestMapping(value = "item.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String setting(String type) {
        UserModel userModel = getUserModel();
        if (userModel.isServerManager()) {
            List<String> ngxDirectory = whitelistDirectoryService.getNgxDirectory(getNode());
            setAttribute("nginx", ngxDirectory);
            setAttribute("type", type);
            JSONObject data = NodeForward.requestData(getNode(), NodeUrl.System_Nginx_item_data, getRequest(), JSONObject.class);
            setAttribute("data", data);
        }
        return "node/system/nginxSetting";
    }


    @RequestMapping(value = "updateNgx", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @UrlPermission(value = Role.NodeManage, optType = UserOperateLogV1.OptType.SaveNginx)
    public String updateNgx() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.System_Nginx_updateNgx).toString();
    }


    @RequestMapping(value = "delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @UrlPermission(value = Role.NodeManage, optType = UserOperateLogV1.OptType.DelNginx)
    public String delete() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.System_Nginx_delete).toString();
    }

}
