package io.jpom.controller.system;

import cn.hutool.http.HttpStatus;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.controller.multipart.MultipartFileBuilder;
import io.jpom.JpomApplication;
import io.jpom.JpomServerApplication;
import io.jpom.common.BaseServerController;
import io.jpom.common.JpomManifest;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.common.interceptor.OptLog;
import io.jpom.model.data.NodeModel;
import io.jpom.model.log.UserOperateLogV1;
import io.jpom.permission.SystemPermission;
import io.jpom.system.ServerConfigBean;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.util.Objects;

/**
 * 在线升级
 *
 * @author bwcx_jzy
 * @date 2019/7/22
 */
@Controller
@RequestMapping(value = "system")
public class SystemUpdateController extends BaseServerController {

//    @RequestMapping(value = "update.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
//    @SystemPermission
//    public String update() {
//        return "system/update";
//    }

    @RequestMapping(value = "info", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @SystemPermission
    public String info() {
        NodeModel nodeModel = tryGetNode();
        if (nodeModel != null) {
            return NodeForward.request(getNode(), getRequest(), NodeUrl.Info).toString();
        }
        return JsonMessage.getString(200, "", JpomManifest.getInstance());
    }

    @RequestMapping(value = "uploadJar.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @OptLog(UserOperateLogV1.OptType.UpdateSys)
    @SystemPermission
    public String uploadJar() throws IOException {
        NodeModel nodeModel = tryGetNode();
        if (nodeModel != null) {
            return NodeForward.requestMultipart(getNode(), getMultiRequest(), NodeUrl.SystemUploadJar).toString();
        }
        //
        Objects.requireNonNull(JpomManifest.getScriptFile());
        MultipartFileBuilder multipartFileBuilder = createMultipart();
        multipartFileBuilder
                .setFileExt("jar")
                .addFieldName("file")
                .setUseOriginalFilename(true)
                .setSavePath(ServerConfigBean.getInstance().getUserTempPath().getAbsolutePath());
        String path = multipartFileBuilder.save();
        // 基础检查
        JsonMessage<String> error = JpomManifest.checkJpomJar(path, JpomServerApplication.class);
        if (error.getCode() != HttpStatus.HTTP_OK) {
            return error.toString();
        }
        String version = error.getMsg();
        JpomManifest.releaseJar(path, version);
        //
        JpomApplication.restart();
        return JsonMessage.getString(200, "升级中大约需要30秒");
    }
}
