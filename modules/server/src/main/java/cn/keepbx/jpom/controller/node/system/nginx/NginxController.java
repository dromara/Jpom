package cn.keepbx.jpom.controller.node.system.nginx;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseController;
import cn.keepbx.jpom.common.Role;
import cn.keepbx.jpom.common.interceptor.UrlPermission;
import cn.keepbx.jpom.model.data.CertModel;
import cn.keepbx.jpom.model.data.UserModel;
import cn.keepbx.jpom.service.system.CertService;
import cn.keepbx.jpom.service.system.NginxService;
import cn.keepbx.jpom.service.system.WhitelistDirectoryService;
import cn.keepbx.jpom.util.CommandUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.odiszapc.nginxparser.NgxBlock;
import com.github.odiszapc.nginxparser.NgxConfig;
import com.github.odiszapc.nginxparser.NgxEntry;
import com.github.odiszapc.nginxparser.NgxParam;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * nginx 管理
 *
 * @author Arno
 */
@Controller
@RequestMapping("/node/system/nginx")
public class NginxController extends BaseController {


    @Resource
    private WhitelistDirectoryService whitelistDirectoryService;
    @Resource
    private CertService certService;

    @Resource
    private NginxService nginxService;

    @RequestMapping(value = "list.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String ngx() {
        JSONArray ngxDirectory = whitelistDirectoryService.getNgxDirectory();
        setAttribute("nginx", ngxDirectory);
        List<CertModel> certList = certService.list();
        setAttribute("cert", certList);
        return "node/system/nginx";
    }

    /**
     * 配置列表
     */
    @RequestMapping(value = "list_data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @UrlPermission(Role.Manage)
    public String list() {
        JSONArray array = nginxService.list();
        return JsonMessage.getString(200, "", array);
    }

    @RequestMapping(value = "item.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String setting(String path, String name, String type) {
        JSONArray ngxDirectory = whitelistDirectoryService.getNgxDirectory();
        setAttribute("nginx", ngxDirectory);
        List<CertModel> certList = certService.list();
        setAttribute("cert", certList);
        setAttribute("type", type);
        name = pathSafe(name);
        if (StrUtil.isNotEmpty(path) && whitelistDirectoryService.checkNgxDirectory(path)) {
            File file = FileUtil.file(path, name);
            JSONObject jsonObject = new JSONObject();
            String string = FileUtil.readUtf8String(file);
            jsonObject.put("context", string);
            jsonObject.put("name", nginxService.paresName(path, file.getAbsolutePath()));
            jsonObject.put("whitePath", path);
            setAttribute("data", jsonObject);
        }
        return "node/system/nginxSetting";
    }


    /**
     * 新增或修改配置
     *
     * @param name      文件名
     * @param whitePath 白名单路径
     */
    @RequestMapping(value = "updateNgx", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @UrlPermission(Role.Manage)
    public String updateNgx(String name, String whitePath, String context, String genre) {
        if (StrUtil.isEmpty(name)) {
            return JsonMessage.getString(400, "请填写文件名");
        }
        if (!name.endsWith(".conf")) {
            return JsonMessage.getString(400, "文件后缀必须为\".conf\"");
        }
        if (!checkPathSafe(name)) {
            return JsonMessage.getString(400, "文件名存在非法字符");
        }
        if (!whitelistDirectoryService.checkNgxDirectory(whitePath)) {
            return JsonMessage.getString(400, "请选择正确的白名单");
        }
        //nginx文件
        File file = FileUtil.file(whitePath, name);
        if ("add".equals(genre) && file.exists()) {
            return JsonMessage.getString(400, "该文件已存在");
        }
        if (StrUtil.isEmpty(context)) {
            return JsonMessage.getString(400, "请填写配置信息");
        }
        InputStream inputStream = new ByteArrayInputStream(context.getBytes());
        try {
            NgxConfig conf = NgxConfig.read(inputStream);
            List<NgxEntry> list = conf.findAll(NgxBlock.class, "server");
            if (list == null || list.size() <= 0) {
                return JsonMessage.getString(404, "内容解析为空");
            }
            for (NgxEntry ngxEntry : list) {
                NgxBlock ngxBlock = (NgxBlock) ngxEntry;
                // 检查日志路径
                NgxParam accessLog = ngxBlock.findParam("access_log");
                if (accessLog != null) {
                    FileUtil.mkParentDirs(accessLog.getValue());
                }
                accessLog = ngxBlock.findParam("error_log");
                if (accessLog != null) {
                    FileUtil.mkParentDirs(accessLog.getValue());
                }
                // 检查证书文件
                NgxParam sslCertificate = ngxBlock.findParam("ssl_certificate");
                if (sslCertificate != null && !FileUtil.exist(sslCertificate.getValue())) {
                    return JsonMessage.getString(404, "证书文件ssl_certificate,不存在");
                }
                NgxParam sslCertificateKey = ngxBlock.findParam("ssl_certificate_key");
                if (sslCertificateKey != null && !FileUtil.exist(sslCertificateKey.getValue())) {
                    return JsonMessage.getString(404, "证书文件ssl_certificate_key,不存在");
                }
                if (!checkRootRole(ngxBlock)) {
                    return JsonMessage.getString(405, "非系统管理员，不能配置静态资源代理");
                }
            }
        } catch (IOException e) {
            DefaultSystemLog.ERROR().error("解析失败", e);
            return JsonMessage.getString(500, "解析失败");
        }
        try {
            FileUtil.writeString(context, file, CharsetUtil.UTF_8);
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
            return JsonMessage.getString(400, "操作失败:" + e.getMessage());
        }
        String msg = this.reloadNginx();
        return JsonMessage.getString(200, "提交成功" + msg);
    }

    private String reloadNginx() {
        try {
            String msg = CommandUtil.execSystemCommand("nginx -s reload");
            if (StrUtil.isNotEmpty(msg)) {
                DefaultSystemLog.LOG().info(msg);
                return "(" + msg + ")";
            }
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error("reload nginx error", e);
        }
        return StrUtil.EMPTY;
    }

    /**
     * 权限检查 防止非系统管理员配置静态资源访问
     *
     * @return false 不正确
     */
    private boolean checkRootRole(NgxBlock ngxBlock) {
        UserModel userModel = getUser();
        List<NgxEntry> locationAll = ngxBlock.findAll(NgxBlock.class, "location");
        if (locationAll != null) {
            for (NgxEntry ngxEntry1 : locationAll) {
                NgxBlock ngxBlock1 = (NgxBlock) ngxEntry1;
                NgxParam locationMain = ngxBlock1.findParam("root");
                if (locationMain == null) {
                    locationMain = ngxBlock1.findParam("alias");
                }
                if (locationMain != null && !userModel.isSystemUser()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 删除配置
     *
     * @param path 文件路径
     */
    @RequestMapping(value = "delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @UrlPermission(Role.Manage)
    public String delete(String path, String name) {
        if (!whitelistDirectoryService.checkNgxDirectory(path)) {
            return JsonMessage.getString(400, "非法操作");
        }
        path = pathSafe(path);
        name = pathSafe(name);
        if (StrUtil.isEmpty(name)) {
            return JsonMessage.getString(400, "删除失败,请正常操作");
        }
        File file = FileUtil.file(path, name);
        try {
            FileUtil.rename(file, file.getName() + "_back", false, true);
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error("删除nginx", e);
            return JsonMessage.getString(400, "删除失败:" + e.getMessage());
        }
        String msg = this.reloadNginx();
        return JsonMessage.getString(200, "删除成功" + msg);
    }

}
