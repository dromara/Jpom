package io.jpom.controller.node.script;

import cn.jiangzeyin.common.JsonMessage;
import com.alibaba.fastjson.JSONArray;
import io.jpom.common.BaseServerController;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.common.interceptor.OptLog;
import io.jpom.model.log.UserOperateLogV1;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.node.script.ScriptServer;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

/**
 * 脚本管理
 *
 * @author jiangzeyin
 * @date 2019/4/24
 */
@Controller
@RequestMapping(value = "/node/script")
@Feature(cls = ClassFeature.SCRIPT)
public class ScriptController extends BaseServerController {

    @Resource
    private ScriptServer scriptServer;

//    @RequestMapping(value = "list.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
//    @Feature(method = MethodFeature.LIST)
//    public String list() {
//        JSONArray jsonArray = scriptServer.listToArray(getNode());
//        setAttribute("array", jsonArray);
//        return "node/script/list";
//    }

    /**
     * @Hotstrip
     * get script list
     * @return
     */
    @RequestMapping(value = "list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String scriptList() {
        JSONArray jsonArray = scriptServer.listToArray(getNode());
        return JsonMessage.getString(200, "success", jsonArray);
    }

//    @RequestMapping(value = "item.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
//    @Feature(method = MethodFeature.EDIT)
//    public String item(String id) {
//        setAttribute("type", "add");
//        if (StrUtil.isNotEmpty(id)) {
//            JSONObject jsonObject = NodeForward.requestData(getNode(), NodeUrl.Script_Item, getRequest(), JSONObject.class);
//            if (jsonObject != null) {
//                setAttribute("type", "edit");
//                setAttribute("item", jsonObject);
//            }
//        }
//        return "node/script/edit";
//    }

    /**
     * 保存脚本
     *
     * @return json
     */
    @RequestMapping(value = "save.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @OptLog(UserOperateLogV1.OptType.Save_Script)
    @Feature(method = MethodFeature.EDIT)
    public String save() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.Script_Save).toString();
    }

    @RequestMapping(value = "del.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @OptLog(UserOperateLogV1.OptType.Save_Del)
    @Feature(method = MethodFeature.DEL)
    public String del() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.Script_Del).toString();
    }

    /**
     * 导入脚本
     *
     * @return json
     */
    @RequestMapping(value = "upload", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @OptLog(UserOperateLogV1.OptType.Save_Upload)
    @Feature(method = MethodFeature.UPLOAD)
    public String upload() {
        return NodeForward.requestMultipart(getNode(), getMultiRequest(), NodeUrl.Script_Upload).toString();
    }
//
//    @RequestMapping(value = "console.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
//    @Feature(method = MethodFeature.EXECUTE)
//    public String console(String id) {
//        return "node/script/console";
//    }
}
