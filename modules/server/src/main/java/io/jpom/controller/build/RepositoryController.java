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
package io.jpom.controller.build;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.Page;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.jpom.build.BuildUtil;
import io.jpom.common.BaseServerController;
import io.jpom.common.Const;
import io.jpom.model.PageResultDto;
import io.jpom.model.data.RepositoryModel;
import io.jpom.model.enums.GitProtocolEnum;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.plugin.IPlugin;
import io.jpom.plugin.PluginFactory;
import io.jpom.service.dblog.BuildInfoService;
import io.jpom.service.dblog.RepositoryService;
import io.jpom.system.JpomRuntimeException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Repository controller
 *
 * @author Hotstrip
 */
@RestController
@Feature(cls = ClassFeature.BUILD_REPOSITORY)
@Slf4j
public class RepositoryController extends BaseServerController {

    private final RepositoryService repositoryService;
    private final BuildInfoService buildInfoService;

    public RepositoryController(RepositoryService repositoryService,
                                BuildInfoService buildInfoService) {
        this.repositoryService = repositoryService;
        this.buildInfoService = buildInfoService;
    }

    /**
     * load repository list
     *
     * <pre>
     *     此请求会分页列出数据，如需要不分页列出所有数据使用{@link #loadRepositoryListAll()}
     * </pre>
     *
     * @return json
     */
    @PostMapping(value = "/build/repository/list")
    @Feature(method = MethodFeature.LIST)
    public Object loadRepositoryList() {
        PageResultDto<RepositoryModel> pageResult = repositoryService.listPage(getRequest());
        return JsonMessage.getString(200, "获取成功", pageResult);
    }

    /**
     * load repository list
     *
     * <pre>
     *     此请求不进行分页列出所有数据，如需要分页使用{@link #loadRepositoryList()}
     * </pre>
     *
     * @return json
     */
    @GetMapping(value = "/build/repository/list_all")
    @Feature(method = MethodFeature.LIST)
    public Object loadRepositoryListAll() {
        List<RepositoryModel> repositoryModels = repositoryService.listByWorkspace(getRequest());
        return JsonMessage.getString(200, "", repositoryModels);
    }

    /**
     * edit
     *
     * @param repositoryModelReq 仓库实体
     * @return json
     */
    @PostMapping(value = "/build/repository/edit")
    @Feature(method = MethodFeature.EDIT)
    public Object editRepository(RepositoryModel repositoryModelReq) {
        this.checkInfo(repositoryModelReq);
        // 检查 rsa 私钥
        boolean andUpdateSshKey = this.checkAndUpdateSshKey(repositoryModelReq);
        Assert.state(andUpdateSshKey, "rsa 私钥文件不存在或者有误");

        if (repositoryModelReq.getRepoType() == RepositoryModel.RepoType.Git.getCode()) {
            RepositoryModel repositoryModel = repositoryService.getByKey(repositoryModelReq.getId(), false);
            if (repositoryModel != null) {
                repositoryModelReq.setRsaPrv(StrUtil.emptyToDefault(repositoryModelReq.getRsaPrv(), repositoryModel.getRsaPrv()));
                repositoryModelReq.setPassword(StrUtil.emptyToDefault(repositoryModelReq.getPassword(), repositoryModel.getPassword()));
            }
            // 验证 git 仓库信息
            try {
                IPlugin plugin = PluginFactory.getPlugin("git-clone");
                Map<String, Object> map = repositoryModelReq.toMap();
                Tuple branchAndTagList = (Tuple) plugin.execute("branchAndTagList", map);
                //Tuple tuple = GitUtil.getBranchAndTagList(repositoryModelReq);
            } catch (JpomRuntimeException jpomRuntimeException) {
                throw jpomRuntimeException;
            } catch (Exception e) {
                log.warn("获取仓库分支失败", e);
                return JsonMessage.toJson(500, "无法连接此仓库，" + e.getMessage());
            }
        }
        if (StrUtil.isEmpty(repositoryModelReq.getId())) {
            // insert data
            repositoryService.insert(repositoryModelReq);
        } else {
            // update data
            //repositoryModelReq.setWorkspaceId(repositoryService.getCheckUserWorkspace(getRequest()));
            repositoryService.updateById(repositoryModelReq, getRequest());
        }

        return JsonMessage.toJson(200, "操作成功");
    }

    /**
     * edit
     *
     * @param id 仓库信息
     * @return json
     */
    @PostMapping(value = "/build/repository/rest_hide_field")
    @Feature(method = MethodFeature.EDIT)
    public Object restHideField(@ValidatorItem String id) {
        RepositoryModel repositoryModel = new RepositoryModel();
        repositoryModel.setId(id);
        repositoryModel.setPassword(StrUtil.EMPTY);
        repositoryModel.setRsaPrv(StrUtil.EMPTY);
        repositoryModel.setRsaPub(StrUtil.EMPTY);
        repositoryModel.setWorkspaceId(repositoryService.getCheckUserWorkspace(getRequest()));
        repositoryService.updateById(repositoryModel);
        return JsonMessage.toJson(200, "操作成功");
    }

    @GetMapping(value = "/build/repository/authorize_repos")
    @Feature(method = MethodFeature.LIST)
    public Object authorizeRepos() {
        // 获取分页信息
        HttpServletRequest request = getRequest();
        Map<String, String> paramMap = ServletUtil.getParamMap(request);
        Page page = repositoryService.parsePage(paramMap);
        String token = paramMap.get("token");
        Assert.hasText(token, "请填写个人令牌");
        String gitlabAddress = StrUtil.blankToDefault(paramMap.get("gitlabAddress"), "https://gitlab.com");
        // 搜索条件
        String condition = paramMap.get("condition");
        // 远程仓库
        String type = paramMap.get("type");
        PageResultDto<JSONObject> pageResultDto;
        switch (type) {
            case "gitee":
                pageResultDto = this.giteeRepos(token, page, condition);
                break;
            case "github":
                // GitHub 不支持条件搜索
                pageResultDto = this.githubRepos(token, page);
                break;
            case "gitlab":
                pageResultDto = this.gitlabRepos(token, page, condition, gitlabAddress);
                break;
            default:
                throw new IllegalArgumentException("不支持的类型");
        }
        return JsonMessage.toJson(HttpStatus.OK.value(), HttpStatus.OK.name(), pageResultDto);
    }

    /**
     * gitlab 仓库
     * <p>
     * https://docs.gitlab.com/ee/api/projects.html#list-all-projects
     *
     * @param token 个人令牌
     * @param page  分页
     * @param gitlabAddress gitLab 地址
     * @return page
     */
    private PageResultDto<JSONObject> gitlabRepos(String token, Page page, String condition, String gitlabAddress) {
        // 删除最后的 /
        if (gitlabAddress.endsWith("/")) {
            gitlabAddress = gitlabAddress.substring(0, gitlabAddress.length() - 1);
        }

        // 内部自建 GitLab，一般都没有配置 https 协议，如果用户没有指定协议，默认走 http，gitlab 走 https
        // https 访问不了的情况下自动替换为 http
        if (!StrUtil.startWithAnyIgnoreCase(gitlabAddress, "http://", "https://")) {
            gitlabAddress = "http://" + gitlabAddress;
        }

        HttpResponse userResponse = null;
        try {
            userResponse = GitLabUtil.getGitLabUserInfo(gitlabAddress, token);
        } catch (IORuntimeException ioRuntimeException) {
            // 连接超时，切换至 http 协议进行重试
            if (StrUtil.startWithIgnoreCase(gitlabAddress, "https")) {
                gitlabAddress = "http" + gitlabAddress.substring(5);
                userResponse = GitLabUtil.getGitLabUserInfo(gitlabAddress, token);
            }
            Assert.state(userResponse != null, "无法连接至 GitLab：" + ioRuntimeException.getMessage());
        }

        Assert.state(userResponse.isOk(), "令牌不正确：" + userResponse.body());
        JSONObject userBody = JSONObject.parseObject(userResponse.body());
        String username = userBody.getString("username");

        Map<String, Object> gitLabRepos = GitLabUtil.getGitLabRepos(gitlabAddress, token, page, condition);

        JSONArray jsonArray = JSONArray.parseArray((String) gitLabRepos.get("body"));
        List<JSONObject> objects = jsonArray.stream().map(o -> {
            JSONObject repo = (JSONObject) o;
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("name", repo.getString("name"));
            String htmlUrl = repo.getString("http_url_to_repo");
            jsonObject.put("url", htmlUrl);
            jsonObject.put("full_name", repo.getString("path_with_namespace"));
            // visibility 有三种：public, internal, or private.（非 public，都是 private）
            jsonObject.put("private", !StrUtil.equalsIgnoreCase("public", repo.getString("visibility")));
            jsonObject.put("description", repo.getString("description"));
            jsonObject.put("username", username);
            jsonObject.put("exists", RepositoryController.this.checkRepositoryUrl(null, htmlUrl));
            return jsonObject;
        }).collect(Collectors.toList());

        PageResultDto<JSONObject> pageResultDto = new PageResultDto<>(page.getPageNumber(), page.getPageSize(), (int) gitLabRepos.get("total"));
        pageResultDto.setResult(objects);
        return pageResultDto;
    }

    /**
     * github 仓库
     *
     * @param token 个人令牌
     * @param page  分页
     * @return page
     */
    private PageResultDto<JSONObject> githubRepos(String token, Page page) {
        GitHubUtil.GitHubUserInfo gitHubUserInfo = GitHubUtil.getGitHubUserInfo(token);
        JSONArray gitHubUserReposArray = GitHubUtil.getGitHubUserRepos(token, page);

        List<JSONObject> objects = gitHubUserReposArray.stream().map(o -> {
            JSONObject repo = (JSONObject) o;
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("name", repo.getString("name"));
            String cloneUrl = repo.getString("clone_url");
            jsonObject.put("url", cloneUrl);
            jsonObject.put("full_name", repo.getString("full_name"));
            jsonObject.put("description", repo.getString("description"));
            jsonObject.put("private", repo.getBooleanValue("private"));
            //
            jsonObject.put("username", gitHubUserInfo.getLogin());
            jsonObject.put("exists", RepositoryController.this.checkRepositoryUrl(null, cloneUrl));
            return jsonObject;
        }).collect(Collectors.toList());
        //
        PageResultDto<JSONObject> pageResultDto = new PageResultDto<>(page.getPageNumber(), page.getPageSize(), gitHubUserInfo.public_repos);
        pageResultDto.setResult(objects);
        return pageResultDto;
    }

    /**
     * gitee 仓库
     *
     * @param token 个人令牌
     * @param page  分页
     * @return page
     */
    private PageResultDto<JSONObject> giteeRepos(String token, Page page, String condition) {
        String giteeUsername = GiteeUtil.getGiteeUsername(token);

        Map<String, Object> giteeReposMap = GiteeUtil.getGiteeRepos(token, page, condition);
        JSONArray jsonArray = (JSONArray) giteeReposMap.get("jsonArray");
        int totalCount = (int) giteeReposMap.get("totalCount");

        List<JSONObject> objects = jsonArray.stream().map(o -> {
            JSONObject repo = (JSONObject) o;
            JSONObject jsonObject = new JSONObject();
            // 项目名称，如：Jpom
            jsonObject.put("name", repo.getString("name"));

            // 项目地址，如：https://gitee.com/dromara/Jpom.git
            String htmlUrl = repo.getString("html_url");
            jsonObject.put("url", htmlUrl);

            // 所属者/项目名，如：dromara/Jpom
            jsonObject.put("full_name", repo.getString("full_name"));

            // 是否为私有仓库，是私有仓库为 true，非私有仓库为 false
            jsonObject.put("private", repo.getBooleanValue("private"));

            // 项目描述，如：简而轻的低侵入式在线构建、自动部署、日常运维、项目监控软件
            jsonObject.put("description", repo.getString("description"));

            jsonObject.put("username", giteeUsername);
            jsonObject.put("exists", this.checkRepositoryUrl(null, htmlUrl));
            return jsonObject;
        }).collect(Collectors.toList());

        PageResultDto<JSONObject> pageResultDto = new PageResultDto<>(page.getPageNumber(), page.getPageSize(), totalCount);
        pageResultDto.setResult(objects);
        return pageResultDto;
    }

    /**
     * 检查信息
     *
     * @param repositoryModelReq 仓库信息
     */
    private void checkInfo(RepositoryModel repositoryModelReq) {
        Assert.notNull(repositoryModelReq, "请输入正确的信息");
        Assert.hasText(repositoryModelReq.getName(), "请填写仓库名称");
        Integer repoType = repositoryModelReq.getRepoType();
        Assert.state(repoType != null && (repoType == RepositoryModel.RepoType.Git.getCode() || repoType == RepositoryModel.RepoType.Svn.getCode()), "请选择仓库类型");
        Assert.hasText(repositoryModelReq.getGitUrl(), "请填写仓库地址");
        //
        Integer protocol = repositoryModelReq.getProtocol();
        Assert.state(protocol != null && (protocol == GitProtocolEnum.HTTP.getCode() || protocol == GitProtocolEnum.SSH.getCode()), "请选择拉取代码的协议");
        // 修正字段
        if (protocol == GitProtocolEnum.HTTP.getCode()) {
            //  http
            repositoryModelReq.setRsaPub(StrUtil.EMPTY);
            repositoryModelReq.setRsaPrv(StrUtil.EMPTY);
        } else if (protocol == GitProtocolEnum.SSH.getCode()) {
            // ssh
            repositoryModelReq.setPassword(StrUtil.emptyToDefault(repositoryModelReq.getPassword(), StrUtil.EMPTY));
        }
        //
        boolean repositoryUrl = this.checkRepositoryUrl(repositoryModelReq.getId(), repositoryModelReq.getGitUrl());
        Assert.state(!repositoryUrl, "已经存在对应的仓库信息啦");
    }

    /**
     * 判断仓库地址是否存在
     *
     * @param id  仓库ID
     * @param url 仓库 url
     * @return true 在当前工作空间已经存在拉
     */
    private boolean checkRepositoryUrl(String id, String url) {
        // 判断仓库是否重复
        Entity entity = Entity.create();
        if (StrUtil.isNotEmpty(id)) {
            Validator.validateGeneral(id, "错误的ID");
            entity.set("id", "<> " + id);
        }
        String workspaceId = repositoryService.getCheckUserWorkspace(getRequest());
        entity.set("workspaceId", workspaceId);
        entity.set("gitUrl", url);
        return repositoryService.exists(entity);
    }

    /**
     * check and update ssh key
     *
     * @param repositoryModelReq 仓库
     */
    private boolean checkAndUpdateSshKey(RepositoryModel repositoryModelReq) {
        if (repositoryModelReq.getProtocol() == GitProtocolEnum.SSH.getCode()) {
            // if rsa key is not empty
            if (StrUtil.isNotEmpty(repositoryModelReq.getRsaPrv())) {
                /**
                 * if rsa key is start with "file:"
                 * copy this file
                 */
                if (StrUtil.startWith(repositoryModelReq.getRsaPrv(), URLUtil.FILE_URL_PREFIX)) {
                    String rsaPath = StrUtil.removePrefix(repositoryModelReq.getRsaPrv(), URLUtil.FILE_URL_PREFIX);
                    if (!FileUtil.exist(rsaPath)) {
                        log.warn("there is no rsa file... {}", rsaPath);
                        return false;
                    }
                } else {
                    //File rsaFile = BuildUtil.getRepositoryRsaFile(repositoryModelReq.getId() + Const.ID_RSA);
                    //  or else put into file
                    //FileUtil.writeUtf8String(repositoryModelReq.getRsaPrv(), rsaFile);
                }
            }
        }
        return true;
    }

    /**
     * delete
     *
     * @param id 仓库ID
     * @return json
     */
    @PostMapping(value = "/build/repository/delete")
    @Feature(method = MethodFeature.DEL)
    public Object delRepository(String id) {
        // 判断仓库是否被关联
        Entity entity = Entity.create();
        entity.set("repositoryId", id);
        boolean exists = buildInfoService.exists(entity);
        Assert.state(!exists, "当前仓库被构建关联，不能直接删除");

        repositoryService.delByKey(id, getRequest());
        File rsaFile = BuildUtil.getRepositoryRsaFile(id + Const.ID_RSA);
        FileUtil.del(rsaFile);
        return JsonMessage.getString(200, "删除成功");
    }

    /**
     * Gitee 工具
     */
    private static class GiteeUtil {

        /**
         * Gitee API 前缀
         */
        private static final String GITEE_API_PREFIX = "https://gitee.com/api";

        /**
         * Gitee API 版本号
         */
        private static final String API_VERSION = "v5";

        /**
         * Gitee API 地址前缀
         */
        private static final String GITEE_API_URL_PREFIX = GITEE_API_PREFIX + "/" + API_VERSION;

        /**
         * 用户授权码
         */
        private static final String ACCESS_TOKEN = "access_token";

        /**
         * 排序方式: 创建时间(created)，更新时间(updated)，最后推送时间(pushed)，仓库所属与名称(full_name)。默认: full_name
         */
        private static final String SORT = "sort";

        /**
         * 当前的页码
         */
        private static final String PAGE = "page";

        /**
         * 每页的数量，最大为 100
         */
        private static final String PER_PAGE = "per_page";

        /**
         * 获取 Gitee 用户名
         *
         * @param token 用户授权码
         * @return Gitee 用户名
         */
        private static String getGiteeUsername(String token) {
            // 参考：https://gitee.com/api/v5/swagger#/getV5User
            HttpResponse userResponse = HttpUtil.createGet(GITEE_API_URL_PREFIX + "/user")
                .form(ACCESS_TOKEN, token)
                .execute();
            Assert.state(userResponse.isOk(), "令牌不正确：" + userResponse.body());
            JSONObject userBody = JSONObject.parseObject(userResponse.body());
            return userBody.getString("login");
        }

        /**
         * 获取 Gitee 用户仓库信息
         *
         * @param token 用户授权码
         * @param page 分页参数
         * @return
         */
        private static Map<String, Object> getGiteeRepos(String token, Page page, String condition) {
            // 参考：https://gitee.com/api/v5/swagger#/getV5UserRepos
            HttpResponse reposResponse = HttpUtil.createGet(GITEE_API_URL_PREFIX + "/user/repos")
                .form(ACCESS_TOKEN, token)
                .form(SORT, "pushed")
                .form(PAGE, page.getPageNumber())
                .form(PER_PAGE, page.getPageSize())
                // 搜索关键字
                .form("q", condition)
                .execute();
            String body = reposResponse.body();
            Assert.state(reposResponse.isOk(), "获取仓库信息错误：" + body);

            // 所有仓库总数，包括公开的和私有的
            String totalCountStr = reposResponse.header("total_count");
            int totalCount = Convert.toInt(totalCountStr, 0);
            //String totalPage = reposResponse.header("total_page");

            Map<String, Object> map = new HashMap<>(2);
            map.put("jsonArray", JSONArray.parseArray(body));
            // 仓库总数
            map.put("totalCount", totalCount);
            return map;
        }
    }

    /**
     * GitHub 工具
     */
    private static class GitHubUtil {

        /**
         * GitHub 用户信息实体类
         *
         * 参考：https://docs.github.com/en/rest/users/users#about-the-users-api
         */
        @Data
        private static class GitHubUserInfo {
            // 只列出目前需要用到的字段

            /**
             * 用户名，如：octocat
             */
            private String login;

            /**
             * 公开仓库数量，如：2
             */
            private int public_repos;

            /**
             * 私有的仓库总数，如：100
             */
            private int total_private_repos;

            /**
             * 拥有的私有仓库，如：100
             */
            private int owned_private_repos;
        }

        /**
         * GitHub 头部
         */
        private static final String GITHUB_HEADER_ACCEPT = "application/vnd.github.v3+json";

        /**
         * GitHub 用户 token 前缀
         */
        private static final String GITHUB_TOKEN = "token ";

        /**
         * GitHub API 前缀
         */
        private static final String GITHUB_API_PREFIX = "https://api.github.com";

        /**
         * 获取 GitHub 用户信息
         *
         * @param token 用户 token
         * @return GitHub 用户信息
         */
        private static GitHubUserInfo getGitHubUserInfo(String token) {
            // 参考：https://docs.github.com/en/rest/users/users#about-the-users-api
            HttpResponse response = HttpUtil
                .createGet(GITHUB_API_PREFIX + "/user")
                .header(Header.ACCEPT, GITHUB_HEADER_ACCEPT)
                .header(Header.AUTHORIZATION, GITHUB_TOKEN + token)
                .execute();
            String body = response.body();
            Assert.state(response.isOk(), "令牌信息错误：" + body);
            return JSONObject.parseObject(body, GitHubUserInfo.class);
        }

        /**
         * 获取 GitHub 仓库信息
         *
         * @param token
         */
        private static JSONArray getGitHubUserRepos(String token, Page page) {
            // 参考：https://docs.github.com/en/rest/repos/repos#list-repositories-for-the-authenticated-user
            HttpResponse response = HttpUtil
                .createGet(GITHUB_API_PREFIX + "/user/repos")
                .header(Header.ACCEPT, GITHUB_HEADER_ACCEPT)
                .header(Header.AUTHORIZATION, GITHUB_TOKEN + token)
                .form("access_token", token)
                .form("sort", "pushed")
                .form("page", page.getPageNumber())
                .form("per_page", page.getPageSize())
                .execute();
            String body = response.body();
            Assert.state(response.isOk(), "拉取仓库信息错误：" + body);
            return JSONArray.parseArray(body);
        }
    }

    /**
     * GitLab 工具
     */
    private static class GitLabUtil {

        /**
         * GitLab 版本号信息，参考：https://docs.gitlab.com/ee/api/version.html
         */
        @Data
        private static class GitLabVersionInfo {

            /**
             * 版本号，如：8.13.0-pre
             */
            private String version;

            /**
             * 修订号，如：4e963fe
             */
            private String revision;

            /**
             * API 版本号，如：v4
             */
            private String apiVersion;
        }

        /**
         * GitLab 版本信息容器，key：GitLab 地址，value：GitLabVersionInfo
         */
        private static final Map<String, GitLabVersionInfo> gitlabVersionMap = new ConcurrentHashMap<>();

        /**
         * 获取 GitLab 版本
         *
         * @param gitlabAddress GitLab 地址
         * @param token 用户 token
         * @return 请求结果
         */
        private static HttpResponse getGitLabVersion(String gitlabAddress, String token, String apiVersion) {
            // 参考：https://docs.gitlab.com/ee/api/version.html
            return HttpUtil.createGet(StrUtil.format("{}/api/{}/version", gitlabAddress, apiVersion))
                .header("PRIVATE-TOKEN", token)
                .execute();
        }

        /**
         * 获取 GitLab 版本信息
         *
         * @param url GitLab 地址
         * @param token 用户 token
         */
        private static GitLabVersionInfo getGitLabVersionInfo(String url, String token) {
            // 缓存中有的话，从缓存读取
            GitLabVersionInfo gitLabVersionInfo = gitlabVersionMap.get(url);
            if (gitLabVersionInfo != null)
                return gitLabVersionInfo;

            // 获取 GitLab 版本号信息
            GitLabVersionInfo glvi = null;
            String apiVersion = "v4";
            HttpResponse v4 = getGitLabVersion(url, token, apiVersion);
            if (v4 != null) {
                glvi = JSON.parseObject(v4.body(), GitLabVersionInfo.class);
            } else {
                apiVersion = "v3";
                HttpResponse v3 = getGitLabVersion(url, token, apiVersion);
                if (v3 != null) {
                    glvi = JSON.parseObject(v3.body(), GitLabVersionInfo.class);
                }
            }

            Assert.state(glvi != null, "获取 GitLab 版本号失败，请检查 GitLab 地址和 token 是否正确");

            // 添加到缓存中
            glvi.setApiVersion(apiVersion);
            gitlabVersionMap.put(url, glvi);

            return glvi;
        }

        /**
         * 获取 GitLab API 版本号
         *
         * @param url GitLab 地址
         * @param token 用户 token
         * @return GitLab API 版本号，如：v4
         */
        private static String getGitLabApiVersion(String url, String token) {
            return getGitLabVersionInfo(url, token).getApiVersion();
        }

        /**
         * 获取 GitLab 用户信息
         *
         * @param gitlabAddress GitLab 地址
         * @param token 用户 token
         * @return 请求结果
         */
        private static HttpResponse getGitLabUserInfo(String gitlabAddress, String token) {
            // 参考：https://docs.gitlab.com/ee/api/users.html
            return HttpUtil.createGet(
                StrUtil.format(
                    "{}/api/{}/user",
                    gitlabAddress,
                    getGitLabApiVersion(gitlabAddress, token)
                )
            )
                .form("access_token", token)
                .timeout(5000)
                .execute();
        }

        /**
         * 获取 GitLab 仓库信息
         *
         * @param gitlabAddress GitLab 地址
         * @param token 用户 token
         * @return 响应结果
         */
        private static Map<String, Object> getGitLabRepos(String gitlabAddress, String token, Page page, String condition) {
            // 参考：https://docs.gitlab.com/ee/api/projects.html
            HttpResponse reposResponse = HttpUtil.createGet(
                StrUtil.format(
                    "{}/api/{}/projects",
                    gitlabAddress,
                    getGitLabApiVersion(gitlabAddress, token)
                )
            )
                .form("private_token", token)
                .form("membership", true)
                // 当 simple=true 时，不返回项目的 visibility，无法判断是私有仓库、公开仓库还是内部仓库
//                .form("simple", true)
                .form("order_by", "updated_at")
                .form("page", page.getPageNumber())
                .form("per_page", page.getPageSize())
                .form("search", condition)
                .execute();

            String body = reposResponse.body();
            Assert.state(reposResponse.isOk(), "拉取仓库信息错误：" + body);

            String totalCountStr = reposResponse.header("X-Total");
            int totalCount = Convert.toInt(totalCountStr, 0);
            //String totalPage = reposResponse.header("total_page");

            Map<String, Object> map = new HashMap<>(2);
            map.put("body", body);
            map.put("total", totalCount);

            return map;
        }
    }

}
