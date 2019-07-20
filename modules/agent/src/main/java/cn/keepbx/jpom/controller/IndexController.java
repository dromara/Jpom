package cn.keepbx.jpom.controller;

import cn.hutool.core.date.BetweenFormater;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.system.SystemUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import cn.keepbx.jpom.common.BaseAgentController;
import cn.keepbx.jpom.common.commander.AbstractProjectCommander;
import cn.keepbx.jpom.common.interceptor.NotAuthorize;
import cn.keepbx.jpom.model.data.ProjectInfoModel;
import cn.keepbx.jpom.model.system.JpomManifest;
import cn.keepbx.jpom.service.WhitelistDirectoryService;
import cn.keepbx.jpom.service.manage.ProjectInfoService;
import cn.keepbx.jpom.system.ConfigBean;
import cn.keepbx.util.JvmUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.File;
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

    /**
     * 缓存信息
     *
     * @return json
     */
    @RequestMapping(value = "cache", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String cache() {
        JSONObject jsonObject = new JSONObject();
        //
        File file = ConfigBean.getInstance().getTempPath();
        String fileSize = FileUtil.readableFileSize(FileUtil.size(file));
        jsonObject.put("fileSize", fileSize);
        //
        jsonObject.put("pidName", AbstractProjectCommander.PID_JPOM_NAME.size());
        jsonObject.put("pidPort", AbstractProjectCommander.PID_PORT.size());
        return JsonMessage.getString(200, "ok", jsonObject);
    }

    /**
     * 清空缓存
     *
     * @return json
     */
    @RequestMapping(value = "clearCache", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String clearCache(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "类型错误") String type) {
        switch (type) {
            case "pidPort":
                AbstractProjectCommander.PID_PORT.clear();
                break;
            case "pidName":
                AbstractProjectCommander.PID_JPOM_NAME.clear();
                break;
            case "fileSize":
                boolean clean = FileUtil.clean(ConfigBean.getInstance().getTempPath());
                if (!clean) {
                    return JsonMessage.getString(504, "清空文件缓存失败");
                }
                break;
            default:
                return JsonMessage.getString(405, "没有对应类型：" + type);

        }
        return JsonMessage.getString(200, "清空成功");
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
        // 运行时间
        long uptime = SystemUtil.getRuntimeMXBean().getUptime();
        String formatBetween = DateUtil.formatBetween(uptime, BetweenFormater.Level.SECOND);
        jsonObject.put("runTime", formatBetween);
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(jsonObject);
        return JsonMessage.getString(200, "", jsonArray);
    }
}
