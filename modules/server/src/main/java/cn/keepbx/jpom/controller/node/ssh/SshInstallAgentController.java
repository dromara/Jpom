package cn.keepbx.jpom.controller.node.ssh;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import cn.jiangzeyin.controller.multipart.MultipartFileBuilder;
import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.common.Type;
import cn.keepbx.jpom.model.data.SshModel;
import cn.keepbx.jpom.service.node.ssh.SshService;
import cn.keepbx.jpom.system.ServerConfigBean;
import cn.keepbx.plugin.ClassFeature;
import cn.keepbx.plugin.Feature;
import cn.keepbx.plugin.MethodFeature;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author bwcx_jzy
 * @date 2019/8/17
 */
@Controller
@RequestMapping(value = "node/ssh")
@Feature(cls = ClassFeature.SSH)
public class SshInstallAgentController extends BaseServerController {

    @Resource
    private SshService sshService;

    @RequestMapping(value = "installAgent.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    @Feature(method = MethodFeature.INSTALL)
    public String installAgent() {
        return "node/ssh/installAgent";
    }

    @RequestMapping(value = "installAgentSubmit.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @Feature(method = MethodFeature.INSTALL)
    public String installAgentSubmit(@ValidatorItem(value = ValidatorRule.NOT_BLANK) String id,
                                     @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "安装路径") String path) throws Exception {
        SshModel sshModel = sshService.getItem(id);
        Objects.requireNonNull(sshModel, "没有找到对应ssh");
        //
        String tempFilePath = ServerConfigBean.getInstance().getUserTempPath().getAbsolutePath();
        MultipartFileBuilder cert = createMultipart().addFieldName("file").setSavePath(tempFilePath);
        String filePath = cert.save();
        //
        File outFle = FileUtil.file(tempFilePath, Type.Agent.name() + "_" + IdUtil.fastSimpleUUID());
        try {
            try (ZipFile zipFile = new ZipFile(filePath)) {
                // 判断文件是否正确
                ZipEntry sh = zipFile.getEntry(Type.Agent.name() + ".sh");
                ZipEntry lib = zipFile.getEntry("lib" + StrUtil.SLASH);
                if (sh == null || null == lib || !lib.isDirectory()) {
                    return JsonMessage.getString(405, "不能jpom 插件包");
                }
                ZipUtil.unzip(zipFile, outFle);
            }
            // 获取上传的tag
            File shFile = FileUtil.file(outFle, Type.Agent.name() + ".sh");
            List<String> lines = FileUtil.readLines(shFile, CharsetUtil.CHARSET_UTF_8);
            String tag = null;
            for (String line : lines) {
                line = line.trim();
                if (StrUtil.startWith(line, "Tag=\"") && StrUtil.endWith(line, "\"")) {
                    tag = line.substring(5, line.length() - 1);
                    break;
                }
            }
            if (StrUtil.isEmpty(tag)) {
                return JsonMessage.getString(405, "管理命令中不存在tag");
            }
            // 查询远程是否运行
            if (sshService.checkSshRun(sshModel, tag)) {
                return JsonMessage.getString(300, "对应服务器中已经存在Jpom 插件端,不需要再次安装啦");
            }
            // 上传文件到服务器
            sshService.uploadDir(sshModel, path, outFle);
            //
            String shPtah = FileUtil.normalize(path + "/" + Type.Agent.name() + ".sh");
            String command = StrUtil.format("sh {} start upgrade", shPtah);
            String result = sshService.exec(sshModel, command);
            return JsonMessage.getString(200, "操作成功:" + result);
        } finally {
            // 清理资源
            FileUtil.del(filePath);
            FileUtil.del(outFle);
        }
    }
}
