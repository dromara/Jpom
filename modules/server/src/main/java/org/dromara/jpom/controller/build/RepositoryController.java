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
package org.dromara.jpom.controller.build;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.io.BomReader;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.text.csv.*;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.Page;
import cn.hutool.extra.servlet.ServletUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import cn.keepbx.jpom.plugins.IPlugin;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.build.BuildUtil;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.ServerConst;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.controller.build.repository.ImportRepoUtil;
import org.dromara.jpom.model.PageResultDto;
import org.dromara.jpom.model.data.RepositoryModel;
import org.dromara.jpom.model.enums.GitProtocolEnum;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.permission.SystemPermission;
import org.dromara.jpom.plugin.PluginFactory;
import org.dromara.jpom.service.dblog.BuildInfoService;
import org.dromara.jpom.service.dblog.RepositoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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
     * @return json
     */
    @PostMapping(value = "/build/repository/list")
    @Feature(method = MethodFeature.LIST)
    public Object loadRepositoryList(HttpServletRequest request) {
        PageResultDto<RepositoryModel> pageResult = repositoryService.listPage(request);
        return JsonMessage.success("获取成功", pageResult);
    }

    /**
     * load build list with params
     *
     * @return json
     */
    @GetMapping(value = "/build/repository/list-group", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<List<String>> getBuildGroupAll() {
        // load list with page
        List<String> group = repositoryService.listGroup();
        return JsonMessage.success("", group);
    }

    /**
     * 下载导入模板
     */
    @GetMapping(value = "/build/repository/import-template", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public void importTemplate(HttpServletResponse response) throws IOException {
        String fileName = "仓库信息导入模板.csv";
        this.setApplicationHeader(response, fileName);
        //
        CsvWriter writer = CsvUtil.getWriter(response.getWriter());
        writer.writeLine("name", "address", "type", "protocol", "share", "private rsa", "username", "password", "timeout(s)");
        writer.flush();
    }

    /**
     * export repository by csv
     */
    @GetMapping(value = "/build/repository/export")
    @Feature(method = MethodFeature.DOWNLOAD)
    @SystemPermission
    public void exportRepositoryList(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String fileName = "导出的 仓库信息 数据 " + DateTime.now().toString(DatePattern.NORM_DATE_FORMAT) + ".csv";
        this.setApplicationHeader(response, fileName);
        CsvWriter writer = CsvUtil.getWriter(response.getWriter());
        int pageInt = 0;
        Map<String, String> paramMap = ServletUtil.getParamMap(request);
        writer.writeLine("name", "group", "address", "type", "protocol", "private rsa", "username", "password", "timeout(s)");
        while (true) {
            // 下一页
            paramMap.put("page", String.valueOf(++pageInt));
            PageResultDto<RepositoryModel> listPage = repositoryService.listPage(paramMap, false);
            if (listPage.isEmpty()) {
                break;
            }
            listPage.getResult()
                .stream()
                .map((Function<RepositoryModel, List<Object>>) repositoryModel -> CollUtil.newArrayList(
                    repositoryModel.getName(),
                    repositoryModel.getGroup(),
                    repositoryModel.getGitUrl(),
                    EnumUtil.likeValueOf(RepositoryModel.RepoType.class, repositoryModel.getRepoType()),
                    EnumUtil.likeValueOf(GitProtocolEnum.class, repositoryModel.getProtocol()),
                    repositoryModel.getRsaPrv(),
                    repositoryModel.getUserName(),
                    repositoryModel.getPassword(),
                    repositoryModel.getTimeout()
                ))
                .map(objects -> objects.stream().map(StrUtil::toStringOrNull).toArray(String[]::new))
                .forEach(writer::writeLine);
            if (ObjectUtil.equal(listPage.getPage(), listPage.getTotalPage())) {
                // 最后一页
                break;
            }
        }
        writer.flush();
    }

    /**
     * 导入数据
     *
     * @return json
     */
    @PostMapping(value = "/build/repository/import-data", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.UPLOAD)
    @SystemPermission
    public IJsonMessage<String> importData(MultipartFile file, HttpServletRequest request) throws IOException {
        Assert.notNull(file, "没有上传文件");
        String originalFilename = file.getOriginalFilename();
        String extName = FileUtil.extName(originalFilename);
        Assert.state(StrUtil.endWithIgnoreCase(extName, "csv"), "不允许的文件格式");
        BomReader bomReader = IoUtil.getBomReader(file.getInputStream());
        CsvReadConfig csvReadConfig = CsvReadConfig.defaultConfig();
        csvReadConfig.setHeaderLineNo(0);
        CsvReader reader = CsvUtil.getReader(bomReader, csvReadConfig);
        CsvData csvData;
        try {
            csvData = reader.read();
        } catch (Exception e) {
            log.error("解析 csv 异常", e);
            return new JsonMessage<>(405, "解析文件异常," + e.getMessage());
        }
        List<CsvRow> rows = csvData.getRows();
        Assert.notEmpty(rows, "没有任何数据");
        int addCount = 0, updateCount = 0;
        for (int i = 0; i < rows.size(); i++) {
            int finalI = i;
            CsvRow csvRow = rows.get(i);
            String name = csvRow.getByName("name");
            Assert.hasText(name, () -> StrUtil.format("第 {} 行 name 字段不能位空", finalI + 1));
            String group = csvRow.getByName("group");
            String address = csvRow.getByName("address");
            Assert.hasText(address, () -> StrUtil.format("第 {} 行 address 字段不能位空", finalI + 1));
            String type = csvRow.getByName("type");
            Assert.hasText(type, () -> StrUtil.format("第 {} 行 type 字段不能位空", finalI + 1));
            RepositoryModel.RepoType repoType = null;
            if ("Git".equalsIgnoreCase(type)) {
                repoType = RepositoryModel.RepoType.Git;
            } else if ("Svn".equalsIgnoreCase(type)) {
                repoType = RepositoryModel.RepoType.Svn;
            }
            Assert.notNull(repoType, () -> StrUtil.format("第 {} 行 type 字段值错误（Git/Svn）", finalI + 1));
            String protocol = csvRow.getByName("protocol");
            Assert.hasText(protocol, () -> StrUtil.format("第 {} 行 protocol 字段不能位空", finalI + 1));
            GitProtocolEnum gitProtocolEnum = null;
            if ("http".equalsIgnoreCase(protocol) || "https".equalsIgnoreCase(protocol)) {
                gitProtocolEnum = GitProtocolEnum.HTTP;
            } else if ("ssh".equalsIgnoreCase(protocol)) {
                gitProtocolEnum = GitProtocolEnum.SSH;
            }
            Assert.notNull(gitProtocolEnum, () -> StrUtil.format("第 {} 行 protocol 字段值错误（http/http/ssh）", finalI + 1));
            String privateRsa = csvRow.getByName("private rsa");
            String username = csvRow.getByName("username");
            String password = csvRow.getByName("password");
            Integer timeout = Convert.toInt(csvRow.getByName("timeout(s)"));
            //
            String optWorkspaceId = repositoryService.covertGlobalWorkspace(request);
            RepositoryModel where = new RepositoryModel();
            where.setProtocol(gitProtocolEnum.getCode());
            where.setGitUrl(address);
            // 工作空间
            where.setWorkspaceId(optWorkspaceId);
            // 查询是否存在
            RepositoryModel repositoryModel = repositoryService.queryByBean(where);
            //
            where.setName(name);
            where.setGroup(group);
            where.setTimeout(timeout);
            where.setPassword(password);
            where.setRsaPrv(privateRsa);
            where.setRepoType(repoType.getCode());
            where.setUserName(username);
            // 检查 rsa 私钥
            boolean andUpdateSshKey = this.checkAndUpdateSshKey(where);
            Assert.state(andUpdateSshKey, StrUtil.format("第 {} 行 rsa 私钥文件不存在或者有误", finalI + 1));
            if (where.getRepoType() == RepositoryModel.RepoType.Git.getCode()) {
                // 验证 git 仓库信息
                try {
                    IPlugin plugin = PluginFactory.getPlugin("git-clone");
                    Map<String, Object> map = where.toMap();
                    Tuple branchAndTagList = (Tuple) plugin.execute("branchAndTagList", map);
                    //Tuple tuple = GitUtil.getBranchAndTagList(repositoryModelReq);
                } catch (Exception e) {
                    log.warn("获取仓库分支失败", e);
                    throw new IllegalStateException(StrUtil.format("第 {} 行 仓库信息有误", finalI + 1));
                }
            }
            if (repositoryModel == null) {
                // 添加
                repositoryService.insert(where);
                addCount++;
            } else {
                where.setId(repositoryModel.getId());
                repositoryService.updateById(where);
                updateCount++;
            }
        }
        return JsonMessage.success("导入成功,添加 {} 条数据,修改 {} 条数据", addCount, updateCount);
    }

    /**
     * load repository list
     *
     * @return json
     */
    @GetMapping(value = "/build/repository/get")
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<RepositoryModel> loadRepositoryGet(String id, HttpServletRequest request) {
        RepositoryModel repositoryModel = repositoryService.getByKey(id, request);
        Assert.notNull(repositoryModel, "没有对应的仓库");
        return JsonMessage.success("", repositoryModel);
    }

    /**
     * 过滤前端多余避免核心字段被更新
     *
     * @param repositoryModelReq 仓库对象
     * @return 可以更新的对象
     */
    private RepositoryModel convertRequest(RepositoryModel repositoryModelReq) {
        RepositoryModel repositoryModel = new RepositoryModel();
        repositoryModel.setName(repositoryModelReq.getName());
        repositoryModel.setGroup(repositoryModelReq.getGroup());
        repositoryModel.setUserName(repositoryModelReq.getUserName());
        repositoryModel.setId(repositoryModelReq.getId());
        repositoryModel.setProtocol(repositoryModelReq.getProtocol());
        repositoryModel.setTimeout(repositoryModelReq.getTimeout());
        repositoryModel.setGitUrl(repositoryModelReq.getGitUrl());
        repositoryModel.setPassword(repositoryModelReq.getPassword());
        repositoryModel.setRepoType(repositoryModelReq.getRepoType());
        repositoryModel.setSortValue(repositoryModelReq.getSortValue());
        repositoryModel.setRsaPrv(repositoryModelReq.getRsaPrv());
        return repositoryModel;
    }

    /**
     * edit
     *
     * @param req 仓库实体
     * @return json
     */
    @PostMapping(value = "/build/repository/edit")
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<String> editRepository(RepositoryModel req, HttpServletRequest request) {
        RepositoryModel repositoryModelReq = this.convertRequest(req);
        repositoryModelReq.setWorkspaceId(repositoryService.covertGlobalWorkspace(request));
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
            repositoryService.getByKeyAndGlobal(repositoryModelReq.getId(), request);
            //repositoryModelReq.setWorkspaceId(repositoryService.getCheckUserWorkspace(getRequest()));
            repositoryService.updateById(repositoryModelReq);
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
    public IJsonMessage<String> restHideField(@ValidatorItem String id, HttpServletRequest request) {
        RepositoryModel byKeyAndGlobal = repositoryService.getByKeyAndGlobal(id, request);
        RepositoryModel repositoryModel = new RepositoryModel();
        repositoryModel.setId(byKeyAndGlobal.getId());
        repositoryModel.setPassword(StrUtil.EMPTY);
        repositoryModel.setRsaPrv(StrUtil.EMPTY);
        repositoryModel.setRsaPub(StrUtil.EMPTY);
        repositoryService.updateById(repositoryModel, request);
        return new JsonMessage<>(200, "操作成功");
    }

    @GetMapping(value = "/build/repository/provider_info")
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<Map<String, Map<String, Object>>> providerInfo() {
        Map<String, Map<String, Object>> providerList = ImportRepoUtil.getProviderList();
        return JsonMessage.success(HttpStatus.OK.name(), providerList);
    }

    @GetMapping(value = "/build/repository/authorize_repos")
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<PageResultDto<JSONObject>> authorizeRepos(HttpServletRequest request,
                                                                 @ValidatorItem String token,
                                                                 String address,
                                                                 @ValidatorItem String type,
                                                                 String condition) {
        // 获取分页信息
        Map<String, String> paramMap = ServletUtil.getParamMap(request);
        Page page = repositoryService.parsePage(paramMap);
        Assert.hasText(token, "请填写个人令牌");
        // 搜索条件
        // 远程仓库
        PageResultDto<JSONObject> pageResultDto;
        ImportRepoUtil.getProviderConfig(type);

        String userName = ImportRepoUtil.getCurrentUserName(type, token, address);
        cn.hutool.json.JSONObject repoList = ImportRepoUtil.getRepoList(type, condition, page, token, userName, address);
        pageResultDto = new PageResultDto<>(page.getPageNumber(), page.getPageSize(), repoList.getLong("total").intValue());
        List<JSONObject> objects = repoList.getJSONArray("data").stream().map(o -> {
            cn.hutool.json.JSONObject obj = (cn.hutool.json.JSONObject) o;
            JSONObject jsonObject = new JSONObject();
            jsonObject.putAll(obj);
            jsonObject.put("exists", RepositoryController.this.checkRepositoryUrl(obj.getStr("url"), request));
            return jsonObject;
        }).collect(Collectors.toList());
        pageResultDto.setResult(objects);
        return JsonMessage.success(HttpStatus.OK.name(), pageResultDto);
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
        entity.set("workspaceId", CollUtil.newArrayList(workspaceId, ServerConst.WORKSPACE_GLOBAL));
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
    public Object delRepository(@ValidatorItem String id, HttpServletRequest request) {
        // 判断仓库是否被关联
        Entity entity = Entity.create();
        entity.set("repositoryId", id);
        boolean exists = buildInfoService.exists(entity);
        Assert.state(!exists, "当前仓库被构建关联，不能直接删除");
        RepositoryModel keyAndGlobal = repositoryService.getByKeyAndGlobal(id, request);
        repositoryService.delByKey(keyAndGlobal.getId());
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
    public IJsonMessage<String> sortItem(@ValidatorItem String id,
                                        @ValidatorItem String method,
                                        String compareId, HttpServletRequest request) {
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
