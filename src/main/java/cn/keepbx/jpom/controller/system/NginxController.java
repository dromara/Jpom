package cn.keepbx.jpom.controller.system;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.controller.multipart.MultipartFileBuilder;
import cn.keepbx.jpom.common.BaseController;
import cn.keepbx.jpom.model.CertModel;
import cn.keepbx.jpom.model.UserModel;
import cn.keepbx.jpom.service.system.CertService;
import cn.keepbx.jpom.service.system.NginxService;
import cn.keepbx.jpom.service.system.WhitelistDirectoryService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.odiszapc.nginxparser.NgxBlock;
import com.github.odiszapc.nginxparser.NgxConfig;
import com.github.odiszapc.nginxparser.NgxDumper;
import com.github.odiszapc.nginxparser.NgxParam;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.io.File;
import java.util.List;

/**
 * nginx 管理
 *
 * @author Arno
 */
@Controller
@RequestMapping("/system")
public class NginxController extends BaseController {


    @Resource
    private WhitelistDirectoryService whitelistDirectoryService;
    @Resource
    private CertService certService;

    @Resource
    private NginxService nginxService;

    @RequestMapping(value = "nginx", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String ngx() {
        JSONArray ngxDirectory = whitelistDirectoryService.getNgxDirectory();
        setAttribute("nginx", ngxDirectory);
        List<CertModel> certList = certService.list();
        setAttribute("cert", certList);
        return "system/nginx";
    }

    @RequestMapping(value = "nginx_setting", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String setting(String path, String name, String type) {
        JSONArray ngxDirectory = whitelistDirectoryService.getNgxDirectory();
        setAttribute("nginx", ngxDirectory);
        List<CertModel> certList = certService.list();
        setAttribute("cert", certList);
        setAttribute("type", type);
        name = pathSafe(name);
        if (StrUtil.isNotEmpty(path) && whitelistDirectoryService.checkNgxDirectory(path)) {
            File file = FileUtil.file(path, name);
            JSONObject jsonObject = nginxService.getItem(file.getPath());
            String string = FileUtil.readUtf8String(file);
            jsonObject.put("context", string);
//            String name = file.getName();
            jsonObject.put("name", nginxService.paresName(path, file.getAbsolutePath()));
            jsonObject.put("whitePath", path);
            setAttribute("data", jsonObject);
        }
        return "system/nginxSetting";
    }

    /**
     * 配置列表
     */
    @RequestMapping(value = "nginx/list", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String list() {
        JSONArray array = nginxService.list();
        return JsonMessage.getString(200, "", array);
    }

    /**
     * 新增或修改配置
     *
     * @param name      文件名
     * @param whitePath 白名单路径
     */
    @RequestMapping(value = "nginx/updateNgx", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String updateNgx(String name, String whitePath, String context, String type, String genre) {
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
        boolean add = "add".equals(genre);
        if ("quick".equals(type)) {
            String port = getParameter("port");
            if (StrUtil.isEmpty(port)) {
                return JsonMessage.getString(400, "请填写监听端口");
            }
            String domain = getParameter("domain");
            if (StrUtil.isEmpty(domain)) {
                return JsonMessage.getString(400, "请填写域名");
            }
            if (!whitePath.endsWith("/")) {
                whitePath += "/";
            }
            String cachePath = whitePath + name.substring(0, name.indexOf(".")) + "/";
            //添加配置信息
            context = getQuickNgx(cachePath, port, domain);
        } else {
            if (StrUtil.isEmpty(context)) {
                return JsonMessage.getString(400, "请填写配置信息");
            }
        }
        try {
            File file = FileUtil.file(whitePath, name);
            if (add) {
                if (file.exists()) {
                    return JsonMessage.getString(400, "该文件已存在");
                }
                File parentFile = file.getParentFile();
                if (!parentFile.exists()) {
                    boolean mkdirs = parentFile.mkdirs();
                    if (!mkdirs) {
                        return JsonMessage.getString(401, "创建" + whitePath + "目录失败，请手动创建");
                    }
                }
            }
            FileUtil.writeString(context, file, CharsetUtil.UTF_8);
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
            if (!add) {
                return JsonMessage.getString(400, "修改失败");
            } else {
                return JsonMessage.getString(400, "新增失败");
            }
        }
        if (add) {
            return JsonMessage.getString(200, "新增成功");
        } else {
            return JsonMessage.getString(200, "修改成功");
        }
    }

    /**
     * 添加配置信息
     *
     * @param cachePath 缓存地址
     * @param port      端口
     * @param domain    监听地址
     */
    private String getQuickNgx(String cachePath, String port, String domain) {
        String location = getParameter("location");
        String convert = getParameter("convert");
        String cert = getParameter("cert");
        String key = getParameter("key");
        String cacheStatus = getParameter("cacheStatus");
        NgxConfig config = new NgxConfig();
        if ("true".equals(cacheStatus)) {
            int cacheSize = getParameterInt("cacheSize", 1024);
            int inactive = getParameterInt("inactive", 30);
            String value = " proxy_cache_path " + cachePath + " levels=1:2 keys_zone=mycache:10m max_size=" + cacheSize +
                    "m inactive=" + inactive + "m use_temp_path=off";
            addNgxParam(config, value);
        }
        NgxBlock sever = new NgxBlock();
        sever.addValue("server");
        addNgxParam(sever, "listen " + port);
        addNgxParam(sever, "server_name " + domain);
        if (StrUtil.isNotEmpty(cert)) {
            addNgxParam(sever, "ssl on");
            addNgxParam(sever, "ssl_certificate " + cert);
            addNgxParam(sever, "ssl_certificate_key " + key);
            addNgxParam(sever, "ssl_prefer_server_ciphers  on");
            addNgxParam(sever, "ssl_session_cache  shared:SSL:1m");
            addNgxParam(sever, "ssl_session_timeout  5m");
            addNgxParam(sever, "ssl_ciphers  HIGH:!aNULL:!MD5");
            if ("true".equals(convert)) {
                NgxBlock httpSever = new NgxBlock();
                httpSever.addValue("server");
                addNgxParam(httpSever, "listen 80");
                addNgxParam(httpSever, "server_name " + domain);
                addNgxParam(httpSever, "rewrite ^(.*)$  https://$host$1 permanent");
                config.addEntry(httpSever);
            }
        }
        NgxBlock locationBlock = new NgxBlock();
        locationBlock.addValue("location /");
        addNgxParam(locationBlock, "proxy_pass " + location);
        addNgxParam(locationBlock, "proxy_set_header  Host            $http_host");
        addNgxParam(locationBlock, "proxy_set_header  X-Real-IP       $remote_addr");
        addNgxParam(locationBlock, "proxy_set_header  X-Forwarded-For $proxy_add_x_forwarded_for");
        sever.addEntry(locationBlock);
        config.addEntry(sever);
        return new NgxDumper(config).dump();
    }

    private void addNgxParam(NgxBlock block, String value) {
        NgxParam ngxParam = new NgxParam();
        ngxParam.addValue(value);
        block.addEntry(ngxParam);
    }

    /**
     * 上传配置文件
     *
     * @param whitePath 白名单路径
     */
    @RequestMapping(value = "nginx/uploadNgx", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String uploadNgx(String whitePath) {
        if (StrUtil.isEmpty(whitePath)) {
            return JsonMessage.getString(401, "请选择对应的白名单");
        }
        if (!whitelistDirectoryService.checkNgxDirectory(whitePath)) {
            return JsonMessage.getString(400, "非法操作");
        }
        try {
            MultipartFileBuilder file = createMultipart().addFieldName("file")
                    .setSavePath(whitePath)
                    .setFileExt("conf")
                    .setUseOriginalFilename(true);
            file.save();
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
            return JsonMessage.getString(400, "上传失败");
        }
        return JsonMessage.getString(200, "上传成功");
    }

    /**
     * 删除配置
     *
     * @param path 文件路径
     */
    @RequestMapping(value = "nginx/delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String delete(String path, String name) {
        if (!whitelistDirectoryService.checkNgxDirectory(path)) {
            return JsonMessage.getString(400, "非法操作");
        }
        UserModel userModel = getUser();
        if (!userModel.isSystemUser()) {
            return JsonMessage.getString(400, "你没有操作权限");
        }
        path = pathSafe(path);
        name = pathSafe(name);
        if (StrUtil.isEmpty(name)) {
            return JsonMessage.getString(400, "删除失败,请正常操作");
        }
        File file = FileUtil.file(path, name);
        boolean delete = file.delete();
        if (!delete) {
            return JsonMessage.getString(400, "删除失败");
        }
        return JsonMessage.getString(200, "删除成功");
    }

}
