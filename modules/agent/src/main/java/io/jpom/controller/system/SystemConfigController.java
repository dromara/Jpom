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
import io.jpom.common.BaseAgentController;
import io.jpom.common.JpomManifest;
import io.jpom.system.ExtConfigBean;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;

/**
 * 系统配置
 *
 * @author bwcx_jzy
 * @date 2019/08/08
 */
@RestController
@RequestMapping(value = "system")
public class SystemConfigController extends BaseAgentController {

    @RequestMapping(value = "getConfig.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String config() throws IOException {
        String content = IoUtil.read(ExtConfigBean.getResource().getInputStream(), CharsetUtil.CHARSET_UTF_8);
        JSONObject json = new JSONObject();
        json.put("content", content);
        return JsonMessage.getString(200, "ok", json);
    }

    @RequestMapping(value = "save_config.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String saveConfig(String content, String restart) {
        if (StrUtil.isEmpty(content)) {
            return JsonMessage.getString(405, "内容不能为空");
        }
        try {
            YamlPropertySourceLoader yamlPropertySourceLoader = new YamlPropertySourceLoader();
            ByteArrayResource resource = new ByteArrayResource(content.getBytes());
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
}
