package io.jpom.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.system.SystemUtil;
import cn.jiangzeyin.common.JsonMessage;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseAgentController;
import io.jpom.common.JpomManifest;
import io.jpom.common.interceptor.NotAuthorize;
import io.jpom.model.data.ProjectInfoModel;
import io.jpom.service.WhitelistDirectoryService;
import io.jpom.service.manage.ProjectInfoService;
import io.jpom.util.JvmUtil;
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
        //  获取JVM中内存总大小
        long totalMemory = SystemUtil.getTotalMemory();
        jsonObject.put("totalMemory", FileUtil.readableFileSize(totalMemory));
        //
        long freeMemory = SystemUtil.getFreeMemory();
        jsonObject.put("freeMemory", FileUtil.readableFileSize(freeMemory));
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
