package cn.keepbx.jpom.controller.node.manage;

import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.common.forward.NodeForward;
import cn.keepbx.jpom.common.forward.NodeUrl;
import cn.keepbx.jpom.model.RunMode;
import cn.keepbx.jpom.model.data.NodeModel;
import cn.keepbx.jpom.model.data.UserModel;
import cn.keepbx.jpom.model.log.UserOperateLogV1;
import cn.keepbx.jpom.service.node.manage.ProjectInfoService;
import cn.keepbx.jpom.service.system.WhitelistDirectoryService;
import cn.keepbx.jpom.system.ConfigBean;
import cn.keepbx.jpom.system.OperateType;
import cn.keepbx.plugin.ClassFeature;
import cn.keepbx.plugin.Feature;
import cn.keepbx.plugin.MethodFeature;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.List;

/**
 * 项目管理
 *
 * @author jiangzeyin
 * @date 2018/9/29
 */
@Controller
@RequestMapping(value = "/node/manage/")
@Feature(cls = ClassFeature.PROJECT)
public class EditProjectController extends BaseServerController {
    @Resource
    private ProjectInfoService projectInfoService;
    @Resource
    private WhitelistDirectoryService whitelistDirectoryService;

    @RequestMapping(value = "getProjectData.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String getProjectData(@ValidatorItem String id) {
        JSONObject projectInfo = projectInfoService.getItem(getNode(), id);
        return JsonMessage.getString(200, "", projectInfo);
    }

    /**
     * 修改项目页面
     *
     * @param id 项目Id
     * @return json
     */
    @RequestMapping(value = "editProject", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public String editProject(String id) {
        JSONObject projectInfo = projectInfoService.getItem(getNode(), id);

        // 白名单
        List<String> jsonArray = whitelistDirectoryService.getProjectDirectory(getNode());
        setAttribute("whitelistDirectory", jsonArray);

        setAttribute("item", projectInfo);
        // 运行模式
        JSONArray runModes = (JSONArray) JSONArray.toJSON(RunMode.values());
        setAttribute("runModes", runModes);
        //
        List<String> hashSet = projectInfoService.getAllGroup(getNode());
        if (hashSet.isEmpty()) {
            hashSet.add("默认");
        }
        setAttribute("groups", hashSet);
        return "node/manage/editProject";
    }


    /**
     * 保存项目
     *
     * @return json
     */
    @RequestMapping(value = "saveProject", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @OperateType(UserOperateLogV1.OptType.SaveProject)
    @Feature(method = MethodFeature.EDIT)
    public String saveProject(String edit, String id) {
        NodeModel nodeModel = getNode();
        UserModel userName = getUser();
        if ("add".equalsIgnoreCase(edit) && !userName.isManage(nodeModel.getId())) {
            return JsonMessage.getString(400, "管理员才能创建项目!");
        }
        if (!userName.isProject(nodeModel.getId(), id)) {
            JsonMessage jsonMessage = new JsonMessage(300, "你没有改项目的权限");
            return jsonMessage.toString();
        }
        // 防止和Jpom冲突
        if (StrUtil.isNotEmpty(ConfigBean.getInstance().applicationTag) && ConfigBean.getInstance().applicationTag.equalsIgnoreCase(id)) {
            return JsonMessage.getString(401, "当前项目id已经被Jpom占用");
        }
        return NodeForward.request(getNode(), getRequest(), NodeUrl.Manage_SaveProject).toString();
    }


    /**
     * 验证lib 暂时用情况
     *
     * @return json
     */
    @RequestMapping(value = "judge_lib.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String saveProject() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.Manage_Jude_Lib).toString();
    }
}
