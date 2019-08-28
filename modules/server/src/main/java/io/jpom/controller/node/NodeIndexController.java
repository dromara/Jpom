package io.jpom.controller.node;

import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.common.JpomManifest;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.SshModel;
import io.jpom.model.data.UserModel;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.node.NodeService;
import io.jpom.service.node.ssh.SshService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.List;

/**
 * 节点管理
 *
 * @author jiangzeyin
 * @date 2019/4/16
 */
@Controller
@RequestMapping(value = "/node")
@Feature(cls = ClassFeature.NODE)
public class NodeIndexController extends BaseServerController {

    @Resource
    private SshService sshService;

    @RequestMapping(value = "list.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    @Feature(method = MethodFeature.LIST)
    public String list() {
        List<NodeModel> nodeModels = nodeService.list();
        setAttribute("array", nodeModels);
        // 获取所有的ssh 名称
        JSONObject sshName = new JSONObject();
        List<SshModel> sshModels = sshService.list();
        if (sshModels != null) {
            sshModels.forEach(sshModel -> sshName.put(sshModel.getId(), sshModel.getName()));
        }
        setAttribute("sshName", sshName);
        return "node/list";
    }

    @RequestMapping(value = "index.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String index() {
        List<NodeModel> nodeModels = nodeService.list();
        setAttribute("array", nodeModels);
        //
        JsonMessage<JpomManifest> jsonMessage = NodeForward.request(getNode(), getRequest(), NodeUrl.Info);
        JpomManifest jpomManifest = jsonMessage.getData(JpomManifest.class);
        setAttribute("jpomManifest", jpomManifest);
        setAttribute("installed", jsonMessage.getCode() == 200);
        UserModel userModel = getUser();
        // 版本提示
        if (!JpomManifest.getInstance().isDebug() && jpomManifest != null && userModel.isSystemUser()) {
            JpomManifest thisInfo = JpomManifest.getInstance();
            if (!StrUtil.equals(jpomManifest.getVersion(), thisInfo.getVersion())) {
                setAttribute("tipUpdate", true);
            }
        }
        return "node/index";
    }

    @RequestMapping(value = "node_status", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @Feature(method = MethodFeature.LIST)
    public String nodeStatus() {
        long timeMillis = System.currentTimeMillis();
        JSONObject jsonObject = NodeForward.requestData(getNode(), NodeUrl.Status, getRequest(), JSONObject.class);
        if (jsonObject == null) {
            return JsonMessage.getString(500, "获取信息失败");
        }
        JSONArray jsonArray = new JSONArray();
        jsonObject.put("timeOut", System.currentTimeMillis() - timeMillis);
        jsonArray.add(jsonObject);
        return JsonMessage.getString(200, "", jsonArray);
    }
}
