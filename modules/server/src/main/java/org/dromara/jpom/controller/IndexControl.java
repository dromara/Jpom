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
package org.dromara.jpom.controller;

import cn.hutool.cache.Cache;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileTypeUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.RegexPool;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.ContentType;
import cn.hutool.system.SystemUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.JsonMessage;
import org.dromara.jpom.common.ServerConst;
import org.dromara.jpom.common.UrlRedirectUtil;
import org.dromara.jpom.common.interceptor.NotLogin;
import org.dromara.jpom.db.DbExtConfig;
import org.dromara.jpom.model.data.NodeModel;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.permission.SystemPermission;
import org.dromara.jpom.plugin.IPlugin;
import org.dromara.jpom.plugin.PluginFactory;
import org.dromara.jpom.service.h2db.BaseWorkspaceService;
import org.dromara.jpom.service.system.SystemParametersServer;
import org.dromara.jpom.service.user.UserBindWorkspaceService;
import org.dromara.jpom.service.user.UserService;
import org.dromara.jpom.system.ExtConfigBean;
import org.dromara.jpom.system.ServerConfig;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 首页
 *
 * @author Administrator
 */
@RestController
@RequestMapping(value = "/")
@Slf4j
public class IndexControl extends BaseServerController {

    private final UserService userService;
    private final UserBindWorkspaceService userBindWorkspaceService;
    private final SystemParametersServer systemParametersServer;
    private final ServerConfig.WebConfig webConfig;
    private final ServerConfig.NodeConfig nodeConfig;
    private final DbExtConfig dbExtConfig;

    public IndexControl(UserService userService,
                        UserBindWorkspaceService userBindWorkspaceService,
                        SystemParametersServer systemParametersServer,
                        ServerConfig serverConfig,
                        DbExtConfig dbExtConfig) {
        this.userService = userService;
        this.userBindWorkspaceService = userBindWorkspaceService;
        this.systemParametersServer = systemParametersServer;
        this.webConfig = serverConfig.getWeb();
        this.nodeConfig = serverConfig.getNode();
        this.dbExtConfig = dbExtConfig;
    }


    /**
     * 加载首页
     *
     * @api {get} / 加载首页 服务端前端页面
     * @apiGroup index
     * @apiSuccess {String} BODY HTML
     */
    @GetMapping(value = {"index", "", "/", "index.html"}, produces = MediaType.TEXT_HTML_VALUE)
    @NotLogin
    public void index(HttpServletResponse response, HttpServletRequest request) {
        this.toIndex(response, request, StrUtil.EMPTY);
    }

    @GetMapping(value = "oauth2-{provide}", produces = MediaType.TEXT_HTML_VALUE)
    @NotLogin
    public void oauth2(HttpServletResponse response, HttpServletRequest request, @PathVariable String provide) {
        this.toIndex(response, request, provide);
    }

    private void toIndex(HttpServletResponse response, HttpServletRequest request, String oauth2Provide) {
        InputStream inputStream = ResourceUtil.getStream("classpath:/dist/index.html");
        String html = IoUtil.read(inputStream, CharsetUtil.CHARSET_UTF_8);
        //<div id="jpomCommonJs"></div>
        String path = ExtConfigBean.getPath();
        File file = FileUtil.file(String.format("%s/script/common.js", path));
        if (file.exists()) {
            String jsCommonContext = FileUtil.readString(file, CharsetUtil.CHARSET_UTF_8);
            // <div id="jpomCommonJs"><!--Don't delete this line, place for public JS --></div>
            String[] commonJsTemps = new String[]{"<div id=\"jpomCommonJs\"><!--Don't delete this line, place for public JS --></div>", "<div id=\"jpomCommonJs\"></div>"};
            for (String item : commonJsTemps) {
                html = StrUtil.replace(html, item, jsCommonContext);
            }
        }

        // <routerBase>
        String proxyPath = UrlRedirectUtil.getHeaderProxyPath(request, ServerConst.PROXY_PATH);
        html = StrUtil.replace(html, "<routerBase>", proxyPath);
        //
        html = StrUtil.replace(html, "<link rel=\"icon\" href=\"favicon.ico\">", "<link rel=\"icon\" href=\"" + proxyPath + "favicon.ico\">");
        // <apiTimeOut>
        int webApiTimeout = webConfig.getApiTimeout();
        html = StrUtil.replace(html, "<apiTimeout>", TimeUnit.SECONDS.toMillis(webApiTimeout) + "");
        html = StrUtil.replace(html, "<uploadFileSliceSize>", nodeConfig.getUploadFileSliceSize() + "");
        html = StrUtil.replace(html, "<uploadFileConcurrent>", nodeConfig.getUploadFileConcurrent() + "");
        html = StrUtil.replace(html, "<oauth2Provide>", oauth2Provide);
        // 修改网页标题
        String title = ReUtil.get("<title>.*?</title>", html, 0);
        if (StrUtil.isNotEmpty(title)) {
            html = StrUtil.replace(html, title, "<title>" + webConfig.getName() + "</title>");
        }
        ServletUtil.write(response, html, ContentType.TEXT_HTML.getValue());
    }

    /**
     * logo 图片
     *
     * @api {get} logo_image logo 图片
     * @apiGroup index
     * @apiSuccess {Object} BODY image
     */
    @RequestMapping(value = "logo_image", method = RequestMethod.GET, produces = MediaType.IMAGE_PNG_VALUE)
    @NotLogin
    public void logoImage(HttpServletResponse response) throws IOException {
        String logoFile = webConfig.getLogoFile();
        this.loadImage(response, logoFile, "classpath:/logo/jpom.png", "jpg", "png", "gif");
    }

    /**
     * logo 图片
     *
     * @api {get} logo_image logo 图片
     * @apiGroup index
     * @apiSuccess {Object} BODY image
     */
    @RequestMapping(value = "favicon.ico", method = RequestMethod.GET, produces = MediaType.IMAGE_PNG_VALUE)
    @NotLogin
    public void favicon(HttpServletResponse response) throws IOException {
        String iconFile = webConfig.getIconFile();
        this.loadImage(response, iconFile, "classpath:/logo/favicon.ico", "ico", "png");
    }

    private void loadImage(HttpServletResponse response, String imgFile, String defaultResource, String... suffix) throws IOException {
        if (StrUtil.isNotEmpty(imgFile)) {
            if (Validator.isMatchRegex(RegexPool.URL_HTTP, imgFile)) {
                // 重定向
                response.sendRedirect(imgFile);
                return;
            }
            File file = FileUtil.file(imgFile);
            if (FileUtil.isFile(file)) {
                String type = FileTypeUtil.getType(file);
                String extName = FileUtil.extName(file);
                if (StrUtil.equalsAnyIgnoreCase(type, suffix) || StrUtil.equalsAnyIgnoreCase(extName, suffix)) {
                    ServletUtil.write(response, file);
                    return;
                }
            }
        }
        // favicon ico
        InputStream inputStream = ResourceUtil.getStream(defaultResource);
        ServletUtil.write(response, inputStream, MediaType.IMAGE_PNG_VALUE);
    }


    /**
     * @return json
     * @author Hotstrip
     * <p>
     * check if need to init system
     * @api {get} check-system 检查是否需要初始化系统
     * @apiGroup index
     * @apiUse defResultJson
     * @apiSuccess {String} routerBase 二级地址
     * @apiSuccess {String} name 系统名称
     * @apiSuccess {String} subTitle 主页面副标题
     * @apiSuccess {String} loginTitle 登录也标题
     * @apiSuccess {String} disabledGuide 是否禁用引导
     * @apiSuccess (222) {Object}  data 系统还没有超级管理员需要初始化
     */
    @NotLogin
    @RequestMapping(value = "check-system", produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonMessage<JSONObject> checkSystem(HttpServletRequest request) {
        JSONObject data = new JSONObject();
        data.put("routerBase", UrlRedirectUtil.getHeaderProxyPath(request, ServerConst.PROXY_PATH));
        //
        data.put("name", webConfig.getName());
        data.put("subTitle", webConfig.getSubTitle());
        data.put("loginTitle", webConfig.getLoginTitle());
        data.put("disabledGuide", webConfig.isDisabledGuide());
        //data.put("disabledCaptcha", webConfig.isDisabledCaptcha());
        data.put("notificationPlacement", webConfig.getNotificationPlacement());
        // 用于判断是否属于容器部署
        boolean inDocker = StrUtil.isNotEmpty(SystemUtil.get("JPOM_PKG"));
        List<String> extendPlugins = new ArrayList<>();
        if (inDocker) {
            extendPlugins.add("inDocker");
        }
        // 验证 git 仓库信息
        try {
            IPlugin plugin = PluginFactory.getPlugin("git-clone");
            Map<String, Object> map = new HashMap<>();
            boolean systemGit = (boolean) plugin.execute("systemGit", map);
            if (systemGit) {
                extendPlugins.add("system-git");
            }
        } catch (Exception e) {
            log.warn("检查 git 客户端异常", e);
        }
        data.put("extendPlugins", extendPlugins);
        if (userService.canUse()) {
            return JsonMessage.success("success", data);
        }
        return new JsonMessage<>(222, "需要初始化系统", data);
    }


    /**
     * @return json
     * @api {post} menus_data.json 获取系统菜单相关数据
     * @apiGroup index
     * @apiUse loginUser
     * @apiParam {String} nodeId 节点ID
     * @apiSuccess {JSON}  data 菜单相关字段
     */
    @RequestMapping(value = "menus_data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonMessage<List<Object>> menusData(HttpServletRequest request) {
        NodeModel nodeModel = tryGetNode();
        UserModel userModel = getUserModel();
        String workspaceId = nodeService.getCheckUserWorkspace(request);
        JSONObject config = systemParametersServer.getConfigDefNewInstance(StrUtil.format("menus_config_{}", workspaceId), JSONObject.class);
        // 菜单
        InputStream inputStream;
        JSONArray showArray;
        if (nodeModel == null) {
            inputStream = ResourceUtil.getStream("classpath:/menus/index.json");
            showArray = config.getJSONArray("serverMenuKeys");
        } else {
            inputStream = ResourceUtil.getStream("classpath:/menus/node-index.json");
            showArray = config.getJSONArray("nodeMenuKeys");
        }

        String json = IoUtil.read(inputStream, CharsetUtil.CHARSET_UTF_8);
        JSONArray jsonArray = JSONArray.parseArray(json);
        List<Object> collect1 = jsonArray.stream().filter(o -> {
            JSONObject jsonObject = (JSONObject) o;
            if (!testMenus(jsonObject, userModel, nodeModel, showArray, request)) {
                return false;
            }
            JSONArray childs = jsonObject.getJSONArray("childs");
            if (childs != null) {
                List<Object> collect = childs.stream().filter(o1 -> {
                    JSONObject jsonObject1 = (JSONObject) o1;
                    return testMenus(jsonObject1, userModel, nodeModel, showArray, request);
                }).collect(Collectors.toList());
                if (collect.isEmpty()) {
                    return false;
                }
                jsonObject.put("childs", collect);
            }
            return true;
        }).collect(Collectors.toList());
        Assert.notEmpty(jsonArray, "没有任何菜单,请联系管理员");
        return JsonMessage.success("", collect1);
    }

    /**
     * @return json
     * @api {post} menus_data.json 获取系统菜单相关数据
     * @apiGroup index
     * @apiUse loginUser
     * @apiParam {String} nodeId 节点ID
     * @apiSuccess {JSON}  data 菜单相关字段
     */
    @RequestMapping(value = "system_menus_data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @SystemPermission
    public JsonMessage<List<Object>> systemMenusData(HttpServletRequest request) {
        UserModel userModel = getUserModel();
        // 菜单
        InputStream inputStream = ResourceUtil.getStream("classpath:/menus/system.json");
        String json = IoUtil.read(inputStream, CharsetUtil.CHARSET_UTF_8);
        JSONArray jsonArray = JSONArray.parseArray(json);
        List<Object> collect1 = jsonArray.stream().filter(o -> {
            JSONObject jsonObject = (JSONObject) o;
            if (!testMenus(jsonObject, userModel, null, null, request)) {
                return false;
            }
            JSONArray childs = jsonObject.getJSONArray("childs");
            if (childs != null) {
                List<Object> collect = childs.stream().filter(o1 -> {
                    JSONObject jsonObject1 = (JSONObject) o1;
                    return testMenus(jsonObject1, userModel, null, null, request);
                }).collect(Collectors.toList());
                if (collect.isEmpty()) {
                    return false;
                }
                jsonObject.put("childs", collect);
            }
            return true;
        }).collect(Collectors.toList());
        Assert.notEmpty(jsonArray, "没有任何菜单,请联系管理员");
        return JsonMessage.success("", collect1);
    }

    private boolean testMenus(JSONObject jsonObject, UserModel userModel, NodeModel nodeModel, JSONArray showArray, HttpServletRequest request) {
        String storageMode = jsonObject.getString("storageMode");
        if (StrUtil.isNotEmpty(storageMode)) {
            if (!StrUtil.equals(dbExtConfig.getMode().name(), storageMode)) {
                return false;
            }
        }
        String role = jsonObject.getString("role");
        if (StrUtil.equals(role, UserModel.SYSTEM_ADMIN) && !userModel.isSuperSystemUser()) {
            // 超级理员权限
            return false;
        }
        // 判断菜单显示
        if (CollUtil.isNotEmpty(showArray) && !userModel.isSuperSystemUser()) {
            String id = jsonObject.getString("id");
            if (!CollUtil.contains(showArray, id)) {
                boolean present = showArray.stream().anyMatch(o -> {
                    String str = StrUtil.toString(o);
                    return StrUtil.startWith(str, id + StrUtil.COLON) || StrUtil.endWith(str, StrUtil.COLON + id);
                });
                if (!present) {
                    return false;
                }
            }
        }
        // 系统管理员权限
        boolean system = StrUtil.equals(role, "system");
        if (system) {
            if (nodeModel == null) {
                return userModel.isSystemUser();
            } else {
                if (userModel.isSuperSystemUser()) {
                    return true;
                }
                String workspaceId = BaseWorkspaceService.getWorkspaceId(request);
                return userBindWorkspaceService.exists(userModel, workspaceId + UserBindWorkspaceService.SYSTEM_USER);
            }
        }
        return true;
    }

    /**
     * 生成分片id
     *
     * @return json
     */
    @GetMapping(value = "generate-sharding-id", produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonMessage<String> generateShardingId() {
        Cache<String, String> shardingIds = BaseServerController.SHARDING_IDS;
        int size = shardingIds.size();
        Assert.state(size <= 100, "分片id最大同时使用 100 个");
        String uuid = IdUtil.fastSimpleUUID();
        shardingIds.put(uuid, uuid);
        return JsonMessage.success(uuid, uuid);
    }
}
