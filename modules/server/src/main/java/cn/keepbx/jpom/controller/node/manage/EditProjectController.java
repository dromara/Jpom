package cn.keepbx.jpom.controller.node.manage;

import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.common.forward.NodeForward;
import cn.keepbx.jpom.common.forward.NodeUrl;
import cn.keepbx.jpom.model.RunMode;
import cn.keepbx.jpom.model.data.UserModel;
import cn.keepbx.jpom.model.data.UserOperateLogV1;
import cn.keepbx.jpom.service.manage.ProjectInfoService;
import cn.keepbx.jpom.service.system.WhitelistDirectoryService;
import cn.keepbx.jpom.system.ConfigBean;
import cn.keepbx.jpom.system.OperateType;
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
 * 项目管理
 *
 * @author jiangzeyin
 * @date 2018/9/29
 */
@Controller
@RequestMapping(value = "/node/manage/")
public class EditProjectController extends BaseServerController {
    @Resource
    private ProjectInfoService projectInfoService;
    @Resource
    private WhitelistDirectoryService whitelistDirectoryService;

    /**
     * 修改项目页面
     *
     * @param id 项目Id
     * @return json
     * @throws IOException IO
     */
    @RequestMapping(value = "editProject", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String editProject(String id) throws IOException {
        JSONObject projectInfo = projectInfoService.getItem(getNode(), id);

        // 白名单
        List<String> jsonArray = whitelistDirectoryService.getProjectDirectory(getNode());
        setAttribute("whitelistDirectory", jsonArray);

        if (projectInfo != null && jsonArray != null) {
            for (Object obj : jsonArray) {
                String path = obj.toString();
                String lib = projectInfo.getString("lib");
                if (lib.startsWith(path)) {
                    String itemWhitelistDirectory = lib.substring(0, path.length());
                    lib = lib.substring(path.length());
                    setAttribute("itemWhitelistDirectory", itemWhitelistDirectory);
                    projectInfo.put("lib", lib);
                    break;
                }
            }
        }
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
    public String saveProject(String edit, String id) {
        if ("add".equalsIgnoreCase(edit)) {
            UserModel userName = getUser();
            if (!userName.isManage(getNode().getId())) {
                return JsonMessage.getString(400, "管理员才能创建项目!");
            }
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
