package cn.keepbx.jpom.controller.node.manage;

import cn.keepbx.jpom.common.BaseNodeController;
import cn.keepbx.jpom.common.forward.NodeForward;
import cn.keepbx.jpom.common.forward.NodeUrl;
import cn.keepbx.jpom.model.data.ProjectInfoModel;
import cn.keepbx.jpom.service.manage.ProjectInfoService;
import cn.keepbx.jpom.service.system.WhitelistDirectoryService;
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
public class EditProjectController extends BaseNodeController {
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
        ProjectInfoModel.RunMode[] runModes = ProjectInfoModel.RunMode.values();
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
     * @param projectInfo 项目实体
     * @return json
     */
    @RequestMapping(value = "saveProject", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String saveProject(ProjectInfoModel projectInfo, String whitelistDirectory, String edit) {
        if ("add".equalsIgnoreCase(edit)) {

        }

        return NodeForward.request(getNode(), getRequest(), NodeUrl.Manage_SaveProject).toString();
//        return save(projectInfo);
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
