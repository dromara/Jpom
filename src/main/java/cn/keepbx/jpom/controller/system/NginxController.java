package cn.keepbx.jpom.controller.system;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.StrSpliter;
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
import com.github.odiszapc.nginxparser.*;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
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

    /**
     * nginx管理
     */
    @RequestMapping(value = "nginx", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String ngx() {
        JSONArray ngxDirectory = whitelistDirectoryService.getNgxDirectory();
        setAttribute("nginx", ngxDirectory);
        List<CertModel> certList = certService.list();
        setAttribute("cert", certList);
        return "system/nginx";
    }

    /**
     * nginx配置
     */
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
        boolean add = false;
        String msg = "修改";
        if ("add".equals(genre)) {
            msg = "新增";
            add = true;
        }
        //nginx文件
        File file = FileUtil.file(whitePath, name);
        if ("quick".equals(type)) {
            String port = getParameter("port");
            if (StrUtil.isEmpty(port)) {
                return JsonMessage.getString(400, "请填写监听端口");
            }
            String domain = getParameter("domain");
            if (StrUtil.isEmpty(domain)) {
                return JsonMessage.getString(400, "请填写域名");
            }
            String location = getParameter("location");
            if (StrUtil.isEmpty(location)) {
                return JsonMessage.getString(400, "请填写代理地址");
            }
            String convert = getParameter("convert");
            String cert = getParameter("cert");
            String key = getParameter("key");
            boolean autoHttps = "true".equalsIgnoreCase(convert);
            context = updateNgxServer(file.getAbsolutePath(), domain, cert, key, autoHttps, location);
        }
        if (StrUtil.isEmpty(context)) {
            return JsonMessage.getString(400, "请填写配置信息");
        }
        try {
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
            return JsonMessage.getString(400, msg + "失败");
        }
        return JsonMessage.getString(200, msg + "成功");
    }

    /**
     * 修改nginx
     *
     * @param path      nginx路径
     * @param domain    域名
     * @param cert      证书地址
     * @param key       私钥地址
     * @param autoHttps 开启http自动跳转到https
     * @param location  代理地址
     */
    private String updateNgxServer(String path, String domain, String cert, String key, boolean autoHttps, String location) {
        //是否增加https证书配置
        boolean addCert = StrUtil.isNotEmpty(cert) || autoHttps;
        try {
            NgxConfig conf = NgxConfig.read(path);
            Collection<NgxEntry> entries = conf.getEntries();
            boolean hasCert = false;
            //将证书指定域名和server监听域名一致的取出来
            List<NgxBlock> ngxEntries = new ArrayList<>();
            NgxConfig config = new NgxConfig();
            for (NgxEntry entry : entries) {
                if (entry instanceof NgxBlock) {
                    NgxBlock block = (NgxBlock) entry;
                    NgxParam serverName = block.findParam("server_name");
                    String values = serverName.getValue();
                    //判断域名是否相等
                    if (checkDomain(values, domain)) {
                        //将域名相同的server取出来
                        ngxEntries.add(block);
                        NgxParam sslCertificate = block.findParam("ssl_certificate");
                        if (null != sslCertificate) {
                            hasCert = true;
                        }
                        continue;
                    }
                }
                config.addEntry(entry);
            }
            //添加证书
            if (addCert) {
                updateNgxCert(config, ngxEntries, hasCert, domain, cert, key, autoHttps, location);
            } else {
                NgxBlock block;
                if (ngxEntries.size() <= 0) {
                    block = new NgxBlock();
                    block.addValue("server");
                } else {
                    block = ngxEntries.get(0);
                }
                config.addEntry(block);
                updateNgxServer(block, domain, location);
            }
            return new NgxDumper(config).dump();
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * 判断域名是否相等
     *
     * @param serverName 配置中的域名
     * @param domain     域名
     */
    private boolean checkDomain(String serverName, String domain) {
        boolean pass = serverName.equalsIgnoreCase(domain);
        if (!pass) {
            List<String> list = StrSpliter.splitTrim(serverName, " ", true);
            List<String> domainList = StrSpliter.splitTrim(domain, " ", true);
            for (String str : domainList) {
                if (list.contains(str)) {
                    pass = true;
                    break;
                }
            }
        }
        return pass;
    }

    /**
     * 修改nginx
     *
     * @param conf      配置信息
     * @param list      监听该域名的server集合
     * @param hasCert   server集合中是否有证书配置
     * @param domain    域名
     * @param cert      证书位置
     * @param key       私钥路径
     * @param autoHttps 开启http自动跳转到https
     * @param location  代理地址
     */
    private void updateNgxCert(NgxConfig conf, List<NgxBlock> list, boolean hasCert, String domain, String
            cert, String key, boolean autoHttps, String location) {
        int size = list.size();
        if (size <= 0) {
            NgxBlock block = new NgxBlock();
            block.addValue("server");
            //修改nginx证书配置
            updateNgxSSlServer(block, domain, cert, key, location);
            conf.addEntry(block);
            if (autoHttps) {
                NgxBlock block1 = addAutoHttps(domain);
                conf.addEntry(block1);
            }
            return;
        }
        if (size < 2 || !hasCert) {
            NgxBlock block = list.get(0);
            updateNgxSSlServer(block, domain, cert, key, location);
            conf.addEntry(block);
        } else {
            for (NgxBlock block : list) {
                NgxParam sslCertificate = block.findParam("ssl_certificate");
                if (null != sslCertificate) {
                    updateNgxSSlServer(block, domain, cert, key, location);
                    conf.addEntry(block);
                    continue;
                }
                if (!autoHttps) {
                    conf.addEntry(block);
                }
            }
        }
        if (autoHttps) {
            NgxBlock block1 = addAutoHttps(domain);
            conf.addEntry(block1);
        }
    }

    /**
     * 修改nginx证书配置
     *
     * @param block  block
     * @param domain 域名
     * @param cert   证书地址
     * @param key    私钥地址
     * @param proxy  代理地址
     */
    private void updateNgxSSlServer(NgxBlock block, String domain, String cert, String key, String proxy) {
        updateNgxBlockParam(block, "443 ssl", "listen");
        updateNgxBlockParam(block, domain, "server_name");
        updateNgxBlockParam(block, "on", "ssl");
        updateNgxBlockParam(block, cert, "ssl_certificate");
        updateNgxBlockParam(block, key, "ssl_certificate_key");
        updateNgxBlockParam(block, "shared:SSL:1m", "ssl_session_cache");
        updateNgxBlockParam(block, "5m", "ssl_session_timeout");
        updateNgxBlockParam(block, "HIGH:!aNULL:!MD5", "ssl_ciphers");
        updateNgxBlockParam(block, "on", "ssl_prefer_server_ciphers");
        NgxBlock location = block.findBlock("location");
        if (location == null) {
            location = new NgxBlock();
            block.addEntry(location);
            location.addValue("location /");
        }
        //修改location
        updateLocation(location, proxy);
    }

    /**
     * 添加http自动跳转到https
     *
     * @param domain 域名
     * @return block
     */
    private NgxBlock addAutoHttps(String domain) {
        NgxBlock block = new NgxBlock();
        block.addValue("server");
        addNgxParam(block, "server_name " + domain);
        addNgxParam(block, "listen 80");
        addNgxParam(block, "rewrite ^(.*)$  https://$host$1 permanent");
        return block;
    }

    /**
     * 修改 server
     *
     * @param block  block
     * @param domain 域名
     * @param proxy  代理地址
     */
    private void updateNgxServer(NgxBlock block, String domain, String proxy) {
        updateNgxBlockParam(block, "80", "listen");
        updateNgxBlockParam(block, domain, "server_name");
        NgxBlock location = block.findBlock("location");
        if (location == null) {
            location = new NgxBlock();
            location.addValue("location /");
            block.addEntry(location);
        }
        //修改location
        updateLocation(location, proxy);
    }

    /**
     * 修改location
     *
     * @param block block
     * @param proxy 代理地址
     */
    private void updateLocation(NgxBlock block, String proxy) {
        updateNgxBlockParam(block, proxy, "proxy_pass");
        updateNgxBlockParam(block, "$http_host", "proxy_set_header", "Host");
        updateNgxBlockParam(block, "$remote_addr", "proxy_set_header", "X-Real-IP");
        updateNgxBlockParam(block, "$proxy_add_x_forwarded_for", "proxy_set_header", "X-Forwarded-For");
    }

    /**
     * 修改参数
     *
     * @param block block
     * @param name  参数名称
     * @param value 参数值
     */
    private void updateNgxBlockParam(NgxBlock block, String value, String... name) {
        NgxParam param = null;
        if (1 == name.length) {
            param = block.findParam(name);
        } else {
            List<NgxEntry> list = block.findAll(NgxParam.class, name);
            for (NgxEntry entry : list) {
                NgxParam ngxParam = (NgxParam) entry;
                List<NgxToken> tokens = (List<NgxToken>) ngxParam.getTokens();
                boolean pass = false;
                for (int i = 0; i < name.length; i++) {
                    String tokenName = name[i].trim();
                    try {
                        String token = tokens.get(i).getToken();
                        if (!tokenName.equals(token)) {
                            pass = false;
                            break;
                        }
                    } catch (Exception e) {
                        pass = false;
                        break;
                    }
                    pass = true;
                }
                if (pass) {
                    param = ngxParam;
                    break;
                }
            }
        }
        String join = StrUtil.join(" ", name);
        if (param == null) {
            param = new NgxParam();
            param.addValue(join + " " + value);
            block.addEntry(param);
        } else {
            param = new NgxParam();
            param.addValue(join + " " + value);
        }
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
