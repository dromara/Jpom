package cn.keepbx.jpom.controller.system;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.http.HttpStatus;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.controller.multipart.MultipartFileBuilder;
import cn.keepbx.jpom.JpomServerApplication;
import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.common.forward.NodeForward;
import cn.keepbx.jpom.common.forward.NodeUrl;
import cn.keepbx.jpom.common.interceptor.UrlPermission;
import cn.keepbx.jpom.model.Role;
import cn.keepbx.jpom.model.data.NodeModel;
import cn.keepbx.jpom.model.log.UserOperateLogV1;
import cn.keepbx.jpom.model.system.JpomManifest;
import cn.keepbx.jpom.system.ServerConfigBean;
import cn.keepbx.util.CommandUtil;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.File;
import java.io.IOException;

/**
 * 在线升级
 *
 * @author bwcx_jzy
 * @date 2019/7/22
 */
@Controller
@RequestMapping(value = "system")
public class SystemUpdateController extends BaseServerController {

    @RequestMapping(value = "update.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String update() {
        return "system/update";
    }

    @RequestMapping(value = "info", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String info() {
        NodeModel nodeModel = tryGetNode();
        if (nodeModel != null) {
            return NodeForward.request(getNode(), getRequest(), NodeUrl.Info).toString();
        }
        return JsonMessage.getString(200, "", JpomManifest.getInstance());
    }

    @RequestMapping(value = "uploadJar.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @UrlPermission(value = Role.System, optType = UserOperateLogV1.OptType.UpdateSys)
    public String uploadJar() throws IOException {
        //
        File scriptFile = JpomManifest.getScriptFile();
        MultipartFileBuilder multipartFileBuilder = createMultipart();
        multipartFileBuilder
                .setFileExt("jar")
                .addFieldName("file")
                .setUseOriginalFilename(true)
                .setSavePath(ServerConfigBean.getInstance().getTempPath().getAbsolutePath());
        String path = multipartFileBuilder.save();
        // 基础检查
        JsonMessage error = JpomManifest.checkJpomJar(path, JpomServerApplication.class);
        if (error.getCode() != HttpStatus.HTTP_OK) {
            return error.toString();
        }
        String version = error.getMsg();
        JpomManifest.releaseJar(path, version);
        ThreadUtil.execute(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                CommandUtil.asyncExeLocalCommand(scriptFile.getParentFile(), FileUtil.getAbsolutePath(scriptFile) + " restart");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        return JsonMessage.getString(200, "升级中大约需要30秒");
    }
}
