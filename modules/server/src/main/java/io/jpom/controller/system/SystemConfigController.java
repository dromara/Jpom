package io.jpom.controller.system;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import com.alibaba.fastjson.JSONObject;
import io.jpom.JpomApplication;
import io.jpom.common.BaseServerController;
import io.jpom.common.JpomManifest;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.common.interceptor.OptLog;
import io.jpom.model.data.SystemIpConfigModel;
import io.jpom.model.log.UserOperateLogV1;
import io.jpom.permission.SystemPermission;
import io.jpom.service.system.SystemIpConfigService;
import io.jpom.system.ExtConfigBean;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 系统配置
 *
 * @author bwcx_jzy
 * @date 2019/08/08
 */
@Controller
@RequestMapping(value = "system")
public class SystemConfigController extends BaseServerController {

    private final SystemIpConfigService systemIpConfigService;

    public SystemConfigController(SystemIpConfigService systemIpConfigService) {
        this.systemIpConfigService = systemIpConfigService;
    }


//    @RequestMapping(value = "config.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
//    @SystemPermission
//    public String config(String nodeId) throws IOException {
//        String content;
//        if (StrUtil.isNotEmpty(nodeId)) {
//            JSONObject jsonObject = NodeForward.requestData(getNode(), NodeUrl.SystemGetConfig, getRequest(), JSONObject.class);
//            content = jsonObject.getString("content");
//        } else {
//            content = IoUtil.read(ExtConfigBean.getResource().getInputStream(), CharsetUtil.CHARSET_UTF_8);
//        }
//        setAttribute("content", content);
//        return "system/config";
//    }

    /**
     * @param nodeId
     * @return
     * @throws IOException
     * @author Hotstrip
     * get server's config or node's config
     * 加载服务端或者节点端配置
     */
    @RequestMapping(value = "config-data", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @SystemPermission
    @ResponseBody
    public String configData(String nodeId) throws IOException {
        String content;
        if (StrUtil.isNotEmpty(nodeId)) {
            JSONObject jsonObject = NodeForward.requestData(getNode(), NodeUrl.SystemGetConfig, getRequest(), JSONObject.class);
            content = jsonObject.getString("content");
        } else {
            content = IoUtil.read(ExtConfigBean.getResource().getInputStream(), CharsetUtil.CHARSET_UTF_8);
        }
        return JsonMessage.getString(200, "加载成功", content);
    }

    @RequestMapping(value = "save_config.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @OptLog(UserOperateLogV1.OptType.EditSysConfig)
    @SystemPermission
    public String saveConfig(String nodeId, String content, String restart) {
        if (StrUtil.isNotEmpty(nodeId)) {
            return NodeForward.request(getNode(), getRequest(), NodeUrl.SystemSaveConfig).toString();
        }
        if (StrUtil.isEmpty(content)) {
            return JsonMessage.getString(405, "内容不能为空");
        }
        try {
            YamlPropertySourceLoader yamlPropertySourceLoader = new YamlPropertySourceLoader();
            ByteArrayResource resource = new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8));
            yamlPropertySourceLoader.load("test", resource);
        } catch (Exception e) {
            DefaultSystemLog.getLog().warn("内容格式错误，请检查修正", e);
            return JsonMessage.getString(500, "内容格式错误，请检查修正:" + e.getMessage());
        }
        if (JpomManifest.getInstance().isDebug()) {
            return JsonMessage.getString(405, "调试模式不支持在线修改,请到resource目录下");
        }
        File resourceFile = ExtConfigBean.getResourceFile();
        FileUtil.writeString(content, resourceFile, CharsetUtil.CHARSET_UTF_8);

        if (Convert.toBool(restart, false)) {
            // 重启
            ThreadUtil.execute(() -> {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {
                }
                JpomApplication.restart();
            });
        }
        return JsonMessage.getString(200, "修改成功");
    }


    /**
     * 加载服务端的 ip 白名单配置
     *
     * @return json
     */
    @RequestMapping(value = "ip-config-data", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @SystemPermission
    @ResponseBody
    public String ipConfigData() {
        SystemIpConfigModel config = systemIpConfigService.getConfig();
        JSONObject jsonObject = new JSONObject();
        if (config != null) {
            jsonObject.put("allowed", config.getAllowed());
            jsonObject.put("prohibited", config.getProhibited());
        }
        jsonObject.put("path", FileUtil.getAbsolutePath(systemIpConfigService.filePath()));
        jsonObject.put("ip", getIp());
        return JsonMessage.getString(200, "加载成功", jsonObject);
    }

    @RequestMapping(value = "save_ip_config.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @OptLog(UserOperateLogV1.OptType.EditSysConfig)
    @SystemPermission
    public String saveIpConfig(String allowed, String prohibited) {
        SystemIpConfigModel systemIpConfigModel = new SystemIpConfigModel();
        systemIpConfigModel.setAllowed(StrUtil.emptyToDefault(allowed, StrUtil.EMPTY));
        systemIpConfigModel.setProhibited(StrUtil.emptyToDefault(prohibited, StrUtil.EMPTY));
        systemIpConfigService.save(systemIpConfigModel);
        //
        return JsonMessage.getString(200, "修改成功");
    }
}
