package cn.keepbx.jpom.controller;

import cn.hutool.system.SystemUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseAgentController;
import cn.keepbx.jpom.common.interceptor.NotAuthorize;
import cn.keepbx.jpom.model.data.ProjectInfoModel;
import cn.keepbx.jpom.common.JpomManifest;
import cn.keepbx.jpom.service.WhitelistDirectoryService;
import cn.keepbx.jpom.service.manage.ProjectInfoService;
import cn.keepbx.util.JvmUtil;
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

    @RequestMapping(value = {"index", "", "index.html", "/"}, produces = MediaType.TEXT_PLAIN_VALUE)
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
        jsonObject.put("osName", SystemUtil.getOsInfo().getName());
        jsonObject.put("jpomVersion", JpomManifest.getInstance().getVersion());
        jsonObject.put("javaVersion", SystemUtil.getJavaRuntimeInfo().getVersion());
        int count = 0;
        if (projectInfoModels != null) {
            count = projectInfoModels.size();
        }
        jsonObject.put("count", count);
        // 运行时间
        jsonObject.put("runTime", JpomManifest.getInstance().getUpTime());
        return JsonMessage.getString(200, "", jsonObject);
    }
}
