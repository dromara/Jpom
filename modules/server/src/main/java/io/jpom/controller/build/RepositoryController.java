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

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.Page;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import io.jpom.build.BuildUtil;
import io.jpom.common.BaseServerController;
import io.jpom.common.JsonMessage;
import io.jpom.common.ServerConst;
import io.jpom.common.validator.ValidatorItem;
import io.jpom.controller.build.repository.GitHubUtil;
import io.jpom.controller.build.repository.GitLabUtil;
import io.jpom.controller.build.repository.GiteaUtil;
import io.jpom.controller.build.repository.GiteeUtil;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import top.jpom.model.PageResultDto;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.List;
import java.util.Map;
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
     *     此请求会分页列出数据，如需要不分页列出所有数据使用{@link #loadRepositoryListAll(HttpServletRequest request)}
     * </pre>
     *
     * @return json
     */
    @PostMapping(value = "/build/repository/list")
    @Feature(method = MethodFeature.LIST)
    public Object loadRepositoryList() {
        PageResultDto<RepositoryModel> pageResult = repositoryService.listPage(getRequest());
        return JsonMessage.success("获取成功", pageResult);
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
    public Object loadRepositoryListAll(HttpServletRequest request) {
        List<RepositoryModel> repositoryModels = repositoryService.listByWorkspace(request);
        return JsonMessage.success("", repositoryModels);
    }

    /**
     * edit
     *
     * @param repositoryModelReq 仓库实体
     * @return json
     */
    @PostMapping(value = "/build/repository/edit")
    @Feature(method = MethodFeature.EDIT)
    public JsonMessage<String> editRepository(RepositoryModel repositoryModelReq, HttpServletRequest request) {
        this.checkInfo(repositoryModelReq, request);
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
                return new JsonMessage<>(500, "无法连接此仓库，" + e.getMessage());
            }
        }
        if (StrUtil.isEmpty(repositoryModelReq.getId())) {
            // insert data
            repositoryService.insert(repositoryModelReq);
        } else {
            // update data
            //repositoryModelReq.setWorkspaceId(repositoryService.getCheckUserWorkspace(getRequest()));
            repositoryService.updateById(repositoryModelReq, request);
        }

        return new JsonMessage<>(200, "操作成功");
    }

    /**
     * edit
     *
     * @param id 仓库信息
     * @return json
     */
    @PostMapping(value = "/build/repository/rest_hide_field")
    @Feature(method = MethodFeature.EDIT)
    public JsonMessage<String> restHideField(@ValidatorItem String id, HttpServletRequest request) {
        RepositoryModel repositoryModel = new RepositoryModel();
        repositoryModel.setId(id);
        repositoryModel.setPassword(StrUtil.EMPTY);
        repositoryModel.setRsaPrv(StrUtil.EMPTY);
        repositoryModel.setRsaPub(StrUtil.EMPTY);
        repositoryModel.setWorkspaceId(repositoryService.getCheckUserWorkspace(request));
        repositoryService.updateById(repositoryModel);
        return new JsonMessage<>(200, "操作成功");
    }

    @GetMapping(value = "/build/repository/authorize_repos")
    @Feature(method = MethodFeature.LIST)
    public JsonMessage<PageResultDto<JSONObject>> authorizeRepos(HttpServletRequest request) {
        // 获取分页信息
        Map<String, String> paramMap = ServletUtil.getParamMap(request);
        Page page = repositoryService.parsePage(paramMap);
        String token = paramMap.get("token");
        Assert.hasText(token, "请填写个人令牌");
        String gitlabAddress = StrUtil.blankToDefault(paramMap.get("gitlabAddress"), "https://gitlab.com");
        String giteaAddress = paramMap.get("giteaAddress");
        // 搜索条件
        String condition = paramMap.get("condition");
        // 远程仓库
        String type = paramMap.get("type");
        PageResultDto<JSONObject> pageResultDto;
        switch (type) {
            case "gitee":
                pageResultDto = this.giteeRepos(token, page, condition, request);
                break;
            case "github":
                // GitHub 不支持条件搜索
                pageResultDto = this.githubRepos(token, page, request);
                break;
            case "gitlab":
                pageResultDto = this.gitlabRepos(token, page, condition, gitlabAddress, request);
                break;
            case "gitea":
                pageResultDto = this.giteaRepos(token, page, condition, giteaAddress, request);
                break;
            default:
                throw new IllegalArgumentException("不支持的类型");
        }
        return new JsonMessage<>(HttpStatus.OK.value(), HttpStatus.OK.name(), pageResultDto);
    }

    /**
     * gitlab 仓库
     * <p>
     * https://docs.gitlab.com/ee/api/projects.html#list-all-projects
     *
     * @param token         个人令牌
     * @param page          分页
     * @param gitlabAddress gitLab 地址
     * @return page
     */
    private PageResultDto<JSONObject> gitlabRepos(String token, Page page, String condition, String gitlabAddress, HttpServletRequest request) {
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
            jsonObject.put("exists", RepositoryController.this.checkRepositoryUrl(htmlUrl, request));
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
    private PageResultDto<JSONObject> githubRepos(String token, Page page, HttpServletRequest request) {
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
            jsonObject.put("exists", RepositoryController.this.checkRepositoryUrl(cloneUrl, request));
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
    private PageResultDto<JSONObject> giteeRepos(String token, Page page, String condition, HttpServletRequest request) {
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
            jsonObject.put("exists", this.checkRepositoryUrl(htmlUrl, request));
            return jsonObject;
        }).collect(Collectors.toList());

        PageResultDto<JSONObject> pageResultDto = new PageResultDto<>(page.getPageNumber(), page.getPageSize(), totalCount);
        pageResultDto.setResult(objects);
        return pageResultDto;
    }

    /**
     * gitea仓库
     *
     * @param token 个人令牌
     * @param page  分页
     * @return page
     */
    private PageResultDto<JSONObject> giteaRepos(String token, Page page, String condition, String giteaAddress, HttpServletRequest request) {
        String giteaUsername = GiteaUtil.getGiteaUsername(giteaAddress, token);

        Map<String, Object> giteaReposMap = GiteaUtil.getGiteaRepos(giteaAddress, token, page, condition);
        JSONArray jsonArray = (JSONArray) giteaReposMap.get("jsonArray");
        int totalCount = (int) giteaReposMap.get("totalCount");

        List<JSONObject> objects = jsonArray.stream().map(o -> {
            JSONObject repo = (JSONObject) o;
            JSONObject jsonObject = new JSONObject();
            // 项目名称，如：Jpom
            jsonObject.put("name", repo.getString("name"));

            // 项目地址，如：https://10.0.0.1:3000/dromara/Jpom.git
            String htmlUrl = repo.getString("html_url");
            jsonObject.put("url", htmlUrl);

            // 所属者/项目名，如：dromara/Jpom
            jsonObject.put("full_name", repo.getString("full_name"));

            // 是否为私有仓库，是私有仓库为 true，非私有仓库为 false
            jsonObject.put("private", repo.getBooleanValue("private"));

            // 项目描述，如：简而轻的低侵入式在线构建、自动部署、日常运维、项目监控软件
            jsonObject.put("description", repo.getString("description"));

            jsonObject.put("username", giteaUsername);
            jsonObject.put("exists", this.checkRepositoryUrl(htmlUrl, request));
            return jsonObject;
        }).collect(Collectors.toList());

        PageResultDto<JSONObject> pageResultDto = new PageResultDto<>(page.getPageNumber(), page.getPageSize(), totalCount);
        pageResultDto.setResult(objects);
        return pageResultDto;
    }

    /**
     * 检查信息
     *
     * @param request            请求信息
     * @param repositoryModelReq 仓库信息
     */
    private void checkInfo(RepositoryModel repositoryModelReq, HttpServletRequest request) {
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
        String workspaceId = repositoryService.getCheckUserWorkspace(request);
        //
        boolean repositoryUrl = this.checkRepositoryUrl(workspaceId, repositoryModelReq.getId(), repositoryModelReq.getGitUrl());
        Assert.state(!repositoryUrl, "已经存在对应的仓库信息啦");
        // 提前处理工作空间ID
        repositoryModelReq.setWorkspaceId(workspaceId);
    }

    /**
     * 判断仓库地址是否存在
     *
     * @param workspaceId 工作空间ID
     * @param id          仓库ID
     * @param url         仓库 url
     * @return true 在当前工作空间已经存在拉
     */
    private boolean checkRepositoryUrl(String workspaceId, String id, String url) {
        // 判断仓库是否重复
        Entity entity = Entity.create();
        if (StrUtil.isNotEmpty(id)) {
            Validator.validateGeneral(id, "错误的ID");
            entity.set("id", "<> " + id);
        }
        entity.set("workspaceId", workspaceId);
        entity.set("gitUrl", url);
        return repositoryService.exists(entity);
    }

    /**
     * 判断仓库地址是否存在
     *
     * @param url 仓库 url
     * @return true 在当前工作空间已经存在拉
     */
    private boolean checkRepositoryUrl(String url, HttpServletRequest request) {
        String workspaceId = repositoryService.getCheckUserWorkspace(request);
        return this.checkRepositoryUrl(workspaceId, null, url);
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
        File rsaFile = BuildUtil.getRepositoryRsaFile(id + ServerConst.ID_RSA);
        FileUtil.del(rsaFile);
        return JsonMessage.success("删除成功");
    }

    /**
     * 排序
     *
     * @param id        节点ID
     * @param method    方法
     * @param compareId 比较的ID
     * @return msg
     */
    @GetMapping(value = "/build/repository/sort-item", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public JsonMessage<String> sortItem(@ValidatorItem String id, @ValidatorItem String method, String compareId) {
        HttpServletRequest request = getRequest();
        if (StrUtil.equalsIgnoreCase(method, "top")) {
            repositoryService.sortToTop(id, request);
        } else if (StrUtil.equalsIgnoreCase(method, "up")) {
            repositoryService.sortMoveUp(id, compareId, request);
        } else if (StrUtil.equalsIgnoreCase(method, "down")) {
            repositoryService.sortMoveDown(id, compareId, request);
        } else {
            return new JsonMessage<>(400, "不支持的方式" + method);
        }
        return new JsonMessage<>(200, "操作成功");
    }

}
