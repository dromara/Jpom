package io.jpom.controller.openapi;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.ServerOpenApi;
import io.jpom.system.ConfigBean;
import io.jpom.system.ServerConfigBean;
import io.jpom.util.JsonFileUtil;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * 获取当前服务端安装id
 *
 * @author bwcx_jzy
 * @date 2019/8/7
 */
@RestController
public class InstallIdController {

    @RequestMapping(value = ServerOpenApi.INSTALL_ID, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String install() throws FileNotFoundException {
        File file = FileUtil.file(ConfigBean.getInstance().getDataPath(), ServerConfigBean.INSTALL);
        if (!file.exists()) {
            return JsonMessage.getString(500, "服务端的安装信息文件不存在");
        }
        JSONObject json = (JSONObject) JsonFileUtil.readJson(file.getAbsolutePath());
        String installId = json.getString("installId");
        if (StrUtil.isEmpty(installId)) {
            return JsonMessage.getString(500, "服务端的安装Id为空");
        }
        return JsonMessage.getString(200, "ok", installId);
    }
}
