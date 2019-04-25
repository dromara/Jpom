package cn.keepbx.jpom.controller.node.script;

import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.common.forward.NodeForward;
import cn.keepbx.jpom.common.forward.NodeUrl;
import cn.keepbx.jpom.common.interceptor.UrlPermission;
import cn.keepbx.jpom.model.Role;
import cn.keepbx.jpom.model.data.UserOperateLogV1;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 脚本管理
 *
 * @author jiangzeyin
 * @date 2019/4/24
 */
@Controller
@RequestMapping(value = "/node/script")
public class ScriptController extends BaseServerController {

    @RequestMapping(value = "list.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String list() {
        JSONArray jsonArray = NodeForward.requestData(getNode(), NodeUrl.Script_List, getRequest(), JSONArray.class);
        setAttribute("array", jsonArray);
        return "node/script/list";
    }


    @RequestMapping(value = "item.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String item(String id) {
        setAttribute("type", "add");
        if (StrUtil.isNotEmpty(id)) {
            JSONObject jsonObject = NodeForward.requestData(getNode(), NodeUrl.Script_Item, getRequest(), JSONObject.class);
            if (jsonObject != null) {
                setAttribute("type", "edit");
                setAttribute("item", jsonObject);
            }
        }
        return "node/script/edit";
    }

    /**
     * 保存脚本
     *
     * @return json
     */
    @RequestMapping(value = "save.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @UrlPermission(value = Role.System, optType = UserOperateLogV1.OptType.Save_Script)
    public String save() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.Script_Save).toString();
    }

    /**
     * 导入脚本
     *
     * @return json
     */
    @RequestMapping(value = "upload", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @UrlPermission(value = Role.System, optType = UserOperateLogV1.OptType.Save_Upload)
    public String upload() {
        return NodeForward.requestMultipart(getNode(), getMultiRequest(), NodeUrl.Script_Upload).toString();
    }

    @RequestMapping(value = "console.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String console(String id) {
        return "node/script/console";
    }
}
