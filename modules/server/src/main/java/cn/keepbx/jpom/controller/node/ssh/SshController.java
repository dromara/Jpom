package cn.keepbx.jpom.controller.node.ssh;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.StrSpliter;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.ssh.JschUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.common.interceptor.UrlPermission;
import cn.keepbx.jpom.model.Role;
import cn.keepbx.jpom.model.data.AgentWhitelist;
import cn.keepbx.jpom.model.data.SshModel;
import cn.keepbx.jpom.model.data.UserModel;
import cn.keepbx.jpom.model.log.UserOperateLogV1;
import cn.keepbx.jpom.service.node.ssh.SshService;
import cn.keepbx.plugin.ClassFeature;
import cn.keepbx.plugin.Feature;
import cn.keepbx.plugin.MethodFeature;
import com.jcraft.jsch.Session;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;

/**
 * @author bwcx_jzy
 * @date 2019/8/9
 */
@Controller
@RequestMapping(value = "node/ssh")
@Feature(cls = ClassFeature.SSH)
public class SshController extends BaseServerController {

    @Resource
    private SshService sshService;

    @RequestMapping(value = "list.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    @Feature(method = MethodFeature.LIST)
    public String list() {
        setAttribute("array", null);
        return "node/ssh/list";
    }

    @RequestMapping(value = "list_data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @Feature(method = MethodFeature.LIST)
    public String listData() throws IOException {
        List<SshModel> list = sshService.list();
        if (list != null) {
            // 不返回密码
            list.forEach(sshModel -> sshModel.setPassword(null));
        }
        return JsonMessage.getString(200, "", list);
    }

    @RequestMapping(value = "save.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @UrlPermission(value = Role.System, optType = UserOperateLogV1.OptType.EditSsh)
    @ResponseBody
    @Feature(method = MethodFeature.EDIT)
    public String save(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "ssh名称不能为空") String name,
                       @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "host不能为空") String host,
                       @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "user不能为空") String user,
                       @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "password不能为空") String password,
                       @ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "port错误") int port,
                       String charset, String fileDirs,
                       String id, String type) throws Exception {
        SshModel sshModel;
        if ("edit".equals(type)) {
            sshModel = sshService.getItem(id);
            if (sshModel == null) {
                return JsonMessage.getString(500, "不存在对应ssh");
            }
        } else {
            sshModel = new SshModel();
        }
        // 目录
        if (StrUtil.isEmpty(fileDirs)) {
            sshModel.setFileDirs(null);
        } else {
            List<String> list = StrSpliter.splitTrim(fileDirs, StrUtil.LF, true);
            if (list != null) {
                for (int i = list.size() - 1; i >= 0; i--) {
                    String s = list.get(i);
                    list.set(i, FileUtil.normalize(s));
                }
            }
            sshModel.setFileDirs(list);
        }
        sshModel.setHost(host);
        sshModel.setPassword(password);
        sshModel.setPort(port);
        sshModel.setUser(user);
        sshModel.setName(name);
        Charset.forName(charset);
        sshModel.setCharset(charset);
        try {
            Session session = JschUtil.openSession(sshModel.getHost(), sshModel.getPort(), sshModel.getUser(), sshModel.getPassword());
            JschUtil.close(session);
        } catch (Exception e) {
            return JsonMessage.getString(505, "ssh连接失败：" + e.getMessage());
        }
        if ("add".equalsIgnoreCase(type)) {
            sshService.addItem(sshModel);
        } else {
            sshService.updateItem(sshModel);
        }
        return JsonMessage.getString(200, "操作成功");
    }

    @RequestMapping(value = "edit.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public String edit(String id) throws IOException {
        if (StrUtil.isNotEmpty(id)) {
            UserModel userModel = getUser();
            SshModel sshModel = sshService.getItem(id);
            if (sshModel != null) {
                if (userModel.isSystemUser()) {
                    setAttribute("item", sshModel);
                }
                //
                String fileDirs = AgentWhitelist.convertToLine(sshModel.getFileDirs());
                setAttribute("fileDirs", fileDirs);
            }
        }
        Collection<Charset> charsets = Charset.availableCharsets().values();
        setAttribute("charsets", charsets);

        return "node/ssh/edit";
    }

    @RequestMapping(value = "terminal.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    @Feature(method = MethodFeature.TERMINAL)
    public String terminal(String id) throws IOException {
        SshModel sshModel = sshService.getItem(id);
        setAttribute("item", sshModel);
        return "node/ssh/terminal";
    }
}
