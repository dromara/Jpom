/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.jpom.controller.system;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.CharPool;
import cn.hutool.core.text.StrSplitter;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.odiszapc.nginxparser.NgxBlock;
import com.github.odiszapc.nginxparser.NgxConfig;
import com.github.odiszapc.nginxparser.NgxEntry;
import com.github.odiszapc.nginxparser.NgxParam;
import io.jpom.common.BaseAgentController;
import io.jpom.common.JsonMessage;
import io.jpom.common.commander.AbstractSystemCommander;
import io.jpom.common.validator.ValidatorItem;
import io.jpom.common.validator.ValidatorRule;
import io.jpom.model.data.AgentWhitelist;
import io.jpom.service.WhitelistDirectoryService;
import io.jpom.service.system.NginxService;
import io.jpom.util.CommandUtil;
import io.jpom.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

/**
 * nginx 列表
 *
 * @author jiangzeyin
 * @since 2019/4/17
 */
@RestController
@RequestMapping("/system/nginx")
@Slf4j
public class NginxController extends BaseAgentController {

    @Resource
    private NginxService nginxService;
    @Resource
    private WhitelistDirectoryService whitelistDirectoryService;

    /**
     * 配置列表
     *
     * @return json
     */
    @RequestMapping(value = "list_data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonMessage<List<JSONObject>> list(String whitePath, String name, String showAll) {
        boolean checkNgxDirectory = whitelistDirectoryService.checkNgxDirectory(whitePath);
        Assert.state(checkNgxDirectory, "文件路径错误,非白名单路径");
        if (StrUtil.isEmpty(name)) {
            name = StrUtil.SLASH;
        }
        List<JSONObject> array = nginxService.list(whitePath, name, showAll);
        return JsonMessage.success("", array);
    }

    /**
     * nginx列表
     */
    @RequestMapping(value = "tree.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonMessage<JSONArray> tree() {
        JSONArray array = nginxService.tree();
        return JsonMessage.success("", array);
    }

    /**
     * 获取配置文件信息页面
     *
     * @param path 白名单路径
     * @param name 名称
     * @return 页面
     */
    @RequestMapping(value = "item_data", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonMessage<JSONObject> itemData(String path, String name) {
        boolean ngxDirectory = whitelistDirectoryService.checkNgxDirectory(path);
        Assert.state(ngxDirectory, "文件路径错误,非白名单路径");

        File file = FileUtil.file(path, name);
        Assert.state(FileUtil.isFile(file), "对应对配置文件不存在");
        JSONObject jsonObject = new JSONObject();
        String string = FileUtil.readUtf8String(file);
        jsonObject.put("context", string);
        String rName = StringUtil.delStartPath(file, path, true);
        // nginxService.paresName(path, file.getAbsolutePath())
        jsonObject.put("name", rName);
        jsonObject.put("whitePath", path);
        return JsonMessage.success("", jsonObject);
//            setAttribute("data", jsonObject);
    }

    private void checkName(String name) {
        Assert.hasText(name, "请填写文件名");
        Assert.state(name.endsWith(".conf"), "文件后缀必须为\".conf\"");
    }

    /**
     * 新增或修改配置
     *
     * @param name      文件名
     * @param whitePath 白名单路径
     * @param genre     操作类型
     * @return json
     */
    @RequestMapping(value = "updateNgx", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonMessage<String> updateNgx(String name, String whitePath, String genre, @ValidatorItem(msg = "请填写配置信息") String context) {
        this.checkName(name);
        //
        boolean ngxDirectory = whitelistDirectoryService.checkNgxDirectory(whitePath);
        Assert.state(ngxDirectory, "请选择正确的白名单");
        //nginx文件
        File file = FileUtil.file(whitePath, name);
        if ("add".equals(genre) && file.exists()) {
            return new JsonMessage<>(400, "该文件已存在");
        }

        InputStream inputStream = new ByteArrayInputStream(context.getBytes());
        try {
            NgxConfig conf = NgxConfig.read(inputStream);
            List<NgxEntry> list = conf.findAll(NgxBlock.class, "server");
            // 取消 nginx 内容必须检测 @jzy 2021-09-11
            //            if (list == null || list.size() <= 0) {
            //                return JsonMessage.getString(404, "内容解析为空");
            //            }
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
                    return new JsonMessage<>(404, "证书文件ssl_certificate,不存在");
                }
                NgxParam sslCertificateKey = ngxBlock.findParam("ssl_certificate_key");
                if (sslCertificateKey != null && !FileUtil.exist(sslCertificateKey.getValue())) {
                    return new JsonMessage<>(404, "证书文件ssl_certificate_key,不存在");
                }
                if (!checkRootRole(ngxBlock)) {
                    return new JsonMessage<>(405, "非系统管理员，不能配置静态资源代理");
                }
            }
        } catch (IOException e) {
            log.error("解析失败", e);
            return new JsonMessage<>(500, "解析失败");
        }

        FileUtil.writeString(context, file, CharsetUtil.UTF_8);

        String msg = this.reloadNginx();
        return JsonMessage.success("提交成功" + msg);
    }

    private String reloadNginx() {
        String serviceName = nginxService.getServiceName();
        try {
            String format = StrUtil.format("{} -s reload", serviceName);
            String nginxPath = this.getNginxPath();
            String msg = StrUtil.isEmpty(nginxPath) ? CommandUtil.execSystemCommand(format) : CommandUtil.execSystemCommand(format, FileUtil.file(nginxPath));
            if (StrUtil.isNotEmpty(msg)) {
                log.info(msg);
                return "(" + msg + ")";
            }
        } catch (Exception e) {
            log.error("reload nginx error", e);
        }
        return StrUtil.EMPTY;
    }

    /**
     * 权限检查 防止非系统管理员配置静态资源访问
     *
     * @param ngxBlock 代码片段
     * @return false 不正确
     */
    private boolean checkRootRole(NgxBlock ngxBlock) {
//        UserModel userModel = getUser();
        //        List<NgxEntry> locationAll = ngxBlock.findAll(NgxBlock.class, "location");
        //        if (locationAll != null) {
        //            for (NgxEntry ngxEntry1 : locationAll) {
        //                NgxBlock ngxBlock1 = (NgxBlock) ngxEntry1;
        //                NgxParam locationMain = ngxBlock1.findParam("root");
        //                if (locationMain == null) {
        //                    locationMain = ngxBlock1.findParam("alias");
        //                }
        //
        //            }
        //        }
        return true;
    }

    /**
     * 删除配置
     *
     * @param path 文件路径
     * @param name 文件名
     * @return json
     */
    @RequestMapping(value = "delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonMessage<String> delete(String path, String name, String type, String from) {
        if (!whitelistDirectoryService.checkNgxDirectory(path)) {
            return new JsonMessage<>(400, "非法操作");
        }
        Assert.hasText(name, "请填写文件名");
        if (StrUtil.equals(from, "back")) {
            Assert.state(name.endsWith(".conf_back"), "不能操作此文件");
        } else {
            Assert.state(name.endsWith(".conf"), "文件后缀必须为\".conf\"");
        }
        File file = FileUtil.file(path, name);
        if (StrUtil.equals(type, "real")) {
            // 真删除
            FileUtil.del(file);
        } else {
            try {
                if (StrUtil.equals(from, "back")) {
                    // 恢复  可能出现 所以 copy Read-only file system
                    File back = FileUtil.file(path, StrUtil.removeSuffix(name, "_back"));
                    FileUtil.copy(file, back, true);
                    FileUtil.del(file);
                } else {
                    // 假删除
                    FileUtil.rename(file, file.getName() + "_back", false, true);
                }
            } catch (Exception e) {
                log.error("删除nginx", e);
                return new JsonMessage<>(400, "操作失败:" + e.getMessage());
            }
        }
        String msg = this.reloadNginx();
        return JsonMessage.success("删除成功" + msg);
    }

    /**
     * 获取nginx状态
     *
     * @return json
     */
    @RequestMapping(value = "status", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonMessage<JSONObject> status() {
        String name = nginxService.getServiceName();
        if (StrUtil.isEmpty(name)) {
            return new JsonMessage<>(500, "服务名错误");
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", name);
        boolean serviceStatus = AbstractSystemCommander.getInstance().getServiceStatus(name);
        jsonObject.put("status", serviceStatus);
        return JsonMessage.success("", jsonObject);
    }

    /**
     * 修改nginx配置
     *
     * @param name 服务名
     * @return json
     */
    @RequestMapping(value = "updateConf", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonMessage<Object> updateConf(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "服务名称错误") String name) {
        JSONObject ngxConf = nginxService.getNgxConf();
        ngxConf.put("name", name);
        nginxService.save(ngxConf);
        return JsonMessage.success("修改成功");
    }

    /**
     * 获取配置信息
     *
     * @return json
     */
    @RequestMapping(value = "config", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonMessage<JSONObject> config() {
        JSONObject ngxConf = nginxService.getNgxConf();
        return JsonMessage.success("", ngxConf);
    }

    /**
     * 启动nginx
     *
     * @return json
     */
    @RequestMapping(value = "open", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonMessage<Object> open() {
        String name = nginxService.getServiceName();
        String result = AbstractSystemCommander.getInstance().startService(name);
        return JsonMessage.success("nginx服务已启动 " + result);
    }

    /**
     * 关闭nginx
     *
     * @return json
     */
    @RequestMapping(value = "close", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonMessage<Object> close() {
        String name = nginxService.getServiceName();
        String result = AbstractSystemCommander.getInstance().stopService(name);
        return JsonMessage.success("nginx服务已停止 " + result);
    }

    private String findServerPath(String name) {
        String checkResult = CommandUtil.execSystemCommand("sc qc " + name);
        List<String> strings = StrSplitter.splitTrim(checkResult, "\n", true);
        //服务路径
        for (String str : strings) {
            str = str.toUpperCase().trim();
            if (str.startsWith("BINARY_PATH_NAME")) {
                String path = str.substring(str.indexOf(CharPool.COLON) + 1).replace("\"", "").trim();
                //file = FileUtil.file(path).getParentFile();
                if (FileUtil.exist(path)) {
                    return path;
                }
            }
        }
        return null;
    }

    private String getNginxPath() {
        AgentWhitelist whitelist = whitelistDirectoryService.getWhitelist();
        return Optional.ofNullable(whitelist)
            .map(AgentWhitelist::getNginxPath)
            .orElseGet(() -> {
                if (SystemUtil.getOsInfo().isWindows()) {
                    String name = nginxService.getServiceName();
                    String path = this.findServerPath(name);
                    if (path != null) {
                        return FileUtil.file(path).getParentFile().getAbsolutePath();
                    }
                }
                return null;
            });
    }

    private String checkNginx() {
        String nginxPath = this.getNginxPath();
        String name = nginxService.getServiceName();
        String format = StrUtil.format("{} -t", name);
        return StrUtil.isEmpty(nginxPath) ? CommandUtil.execSystemCommand(format) : CommandUtil.execSystemCommand(format, FileUtil.file(nginxPath));
    }

    /**
     * 重新加载
     *
     * @return json
     */
    @RequestMapping(value = "reload", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonMessage<String> reload() {
        String checkResult = this.checkNginx();
        if (StrUtil.isNotEmpty(checkResult) && !StrUtil.containsIgnoreCase(checkResult, "successful")) {
            return new JsonMessage<>(400, checkResult);
        }
        String reloadMsg = this.reloadNginx();
        return JsonMessage.success("重新加载成功:" + reloadMsg, checkResult);
    }
}
