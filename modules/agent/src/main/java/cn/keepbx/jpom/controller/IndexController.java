package cn.keepbx.jpom.controller;

import cn.hutool.system.SystemUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.BaseJpomApplication;
import cn.keepbx.jpom.common.BaseAgentController;
import cn.keepbx.jpom.common.interceptor.NotAuthorize;
import cn.keepbx.jpom.model.data.ProjectInfoModel;
import cn.keepbx.jpom.model.system.JpomManifest;
import cn.keepbx.jpom.service.WhitelistDirectoryService;
import cn.keepbx.jpom.service.manage.ProjectInfoService;
import cn.keepbx.jpom.util.JvmUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 首页
 *
 * @author jiangzeyin
 * @date 2019/4/17
 */
@RestController
public class IndexController extends BaseAgentController {
    @Resource
    private WhitelistDirectoryService whitelistDirectoryService;
    @Resource
    private ProjectInfoService projectInfoService;

    @RequestMapping(value = {"index", "", "index.html", "/"}, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @NotAuthorize
    public String index() {
        return "Jpom-Agent";
    }

    @RequestMapping(value = "info", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String info() {
        int code;
        if (whitelistDirectoryService.isInstalled()) {
            code = 200;
        } else {
            code = 201;
        }
        return JsonMessage.getString(code, "", JpomManifest.getInstance());
    }

    /**
     * 返回节点项目状态信息
     *
     * @return array
     */
    @RequestMapping(value = "status", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String status() {
        List<ProjectInfoModel> projectInfoModels = projectInfoService.list();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("javaVirtualCount", JvmUtil.getJavaVirtualCount());
        jsonObject.put("osName", BaseJpomApplication.OS_INFO.getName());
        jsonObject.put("jpomVersion", JpomManifest.getInstance().getVersion());
        jsonObject.put("javaVersion", SystemUtil.getJavaRuntimeInfo().getVersion());

        if (projectInfoModels == null) {
            jsonObject.put("count", 0);
            jsonObject.put("runCount", 0);
            jsonObject.put("stopCount", 0);
            return JsonMessage.getString(200, "", jsonObject);
        }
        int count = 0, runCount = 0, stopCount = 0;
        for (ProjectInfoModel projectInfoModel : projectInfoModels) {
            count++;
            if (projectInfoModel.isStatus(true)) {
                runCount++;
            } else {
                stopCount++;
            }
        }
        jsonObject.put("count", count);
        jsonObject.put("runCount", runCount);
        jsonObject.put("stopCount", stopCount);
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(jsonObject);
        return JsonMessage.getString(200, "", jsonArray);
    }
}
