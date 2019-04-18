package cn.keepbx.jpom.controller.manage;

import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseJpomController;
import cn.keepbx.jpom.common.commander.AbstractProjectCommander;
import cn.keepbx.jpom.model.data.ProjectInfoModel;
import cn.keepbx.jpom.service.manage.ProjectInfoService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.List;

/**
 * @author jiangzeyin
 * @date 2019/4/16
 */
@RestController
@RequestMapping(value = "/manage/")
public class ProjectListController extends BaseJpomController {

    @Resource
    private ProjectInfoService projectInfoService;

    @RequestMapping(value = "getProjectItem", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getProjectItem(String id) {
        return JsonMessage.getString(200, "", projectInfoService.getItem(id));
    }

    @RequestMapping(value = "getProjectGroup", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getProjectGroup() {
        HashSet<String> strings = projectInfoService.getAllGroup();
        return JsonMessage.getString(200, "", strings);
    }

    /**
     * 程序项目信息
     *
     * @param group 分组
     * @return json
     */
    @RequestMapping(value = "getProjectInfo", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getProjectInfo(String group, String notStatus) {
        try {
            boolean status = StrUtil.isEmpty(notStatus);
            // 查询数据
            List<ProjectInfoModel> projectInfoModels = projectInfoService.list();
            // 转换为数据
            JSONArray array = new JSONArray();
            for (ProjectInfoModel projectInfoModel : projectInfoModels) {
                if (StrUtil.isNotEmpty(group) && !group.equals(projectInfoModel.getGroup())) {
                    continue;
                }
                JSONObject object = projectInfoModel.toJson();
                if (status) {
                    object.put("status", projectInfoModel.isStatus(true));
                }
                array.add(object);
            }
            array.sort((oo1, oo2) -> {
                JSONObject o1 = (JSONObject) oo1;
                JSONObject o2 = (JSONObject) oo2;
                String group1 = o1.getString("group");
                String group2 = o2.getString("group");
                if (group1 == null || group2 == null) {
                    return -1;
                }
                return group1.compareTo(group2);
            });
            return JsonMessage.getString(200, "查询成功！", array);
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
            return JsonMessage.getString(500, e.getMessage());
        }
    }

    @RequestMapping(value = "getProjectPort", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getProjectGroup(String ids) {
        if (StrUtil.isEmpty(ids)) {
            return JsonMessage.getString(400, "");
        }
        JSONArray jsonArray = JSONArray.parseArray(ids);
        JSONObject jsonObject = new JSONObject();
        JSONObject itemObj;
        for (Object object : jsonArray) {
            String item = object.toString();
            int pid;
            try {
                pid = AbstractProjectCommander.getInstance().getPid(item);
            } catch (Exception e) {
                DefaultSystemLog.ERROR().error("获取端口错误", e);
                continue;
            }
            if (pid <= 0) {
                continue;
            }
            itemObj = new JSONObject();
            String port = AbstractProjectCommander.getInstance().getMainPort(pid);
            itemObj.put("port", port);
            itemObj.put("pid", pid);
            jsonObject.put(item, itemObj);
        }
        return JsonMessage.getString(200, "", jsonObject);
    }

    @RequestMapping(value = "getRunModes", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getRunModes() {
        ProjectInfoModel.RunMode[] runModes = ProjectInfoModel.RunMode.values();
        return JsonMessage.getString(200, "", runModes);
    }
}
