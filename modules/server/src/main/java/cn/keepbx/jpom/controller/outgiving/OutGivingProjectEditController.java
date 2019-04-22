package cn.keepbx.jpom.controller.outgiving;

import cn.hutool.http.HttpUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.model.RunMode;
import cn.keepbx.jpom.model.data.NodeModel;
import cn.keepbx.jpom.model.data.UserModel;
import cn.keepbx.jpom.service.system.ServerWhitelistServer;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;

/**
 * 节点分发编辑项目
 *
 * @author jiangzeyin
 * @date 2019/4/22
 */
@Controller
@RequestMapping(value = "/outgiving")
public class OutGivingProjectEditController extends BaseServerController {
    @Resource
    private ServerWhitelistServer serverWhitelistServer;

    @RequestMapping(value = "editProject", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String addOutgiving(String id) throws IOException {
        // 运行模式
        JSONArray runModes = (JSONArray) JSONArray.toJSON(RunMode.values());
        setAttribute("runModes", runModes);

        UserModel userModel = getUser();
        if (userModel.isServerManager()) {
            List<NodeModel> nodeModels = nodeService.listAndProject();
            setAttribute("nodeModels", nodeModels);
            //
            String reqId = nodeService.cacheNodeList(nodeModels);
            setAttribute("reqId", reqId);
        }

        // 白名单
        List<String> jsonArray = serverWhitelistServer.getOutGiving();
        setAttribute("whitelistDirectory", jsonArray);

        return "outgiving/editProject";
    }


    @RequestMapping(value = "save", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String save(String id) throws IOException {

        JSONObject jsonObject = new JSONObject();
        HttpUtil.createGet("").form(jsonObject);
        return JsonMessage.getString(400, "error");
    }
}
