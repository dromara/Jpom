/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
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
import org.dromara.jpom.common.i18n.I18nMessageUtil;
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
        return JsonMessage.success(I18nMessageUtil.get("i18n.get_success.fb55"), pageResult);
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
        String fileName = I18nMessageUtil.get("i18n.repository_import_template.5e2d");
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
        String prex  = I18nMessageUtil.get("i18n.exported_repo_data.bac5");
        String fileName = prex + DateTime.now().toString(DatePattern.NORM_DATE_FORMAT) + ".csv";
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
        Assert.notNull(file, I18nMessageUtil.get("i18n.no_uploaded_file.07ef"));
        String originalFilename = file.getOriginalFilename();
        String extName = FileUtil.extName(originalFilename);
        boolean csv = StrUtil.endWithIgnoreCase(extName, "csv");
        Assert.state(csv, I18nMessageUtil.get("i18n.disallowed_file_format.d6e4"));
        BomReader bomReader = IoUtil.getBomReader(file.getInputStream());
        CsvReadConfig csvReadConfig = CsvReadConfig.defaultConfig();
        csvReadConfig.setHeaderLineNo(0);
        CsvReader reader = CsvUtil.getReader(bomReader, csvReadConfig);
        CsvData csvData;
        try {
            csvData = reader.read();
        } catch (Exception e) {
            log.error(I18nMessageUtil.get("i18n.parse_csv_exception.885e"), e);
            return new JsonMessage<>(405, I18nMessageUtil.get("i18n.parse_file_exception.374d") + e.getMessage());
        }
        List<CsvRow> rows = csvData.getRows();
        Assert.notEmpty(rows, I18nMessageUtil.get("i18n.no_data.55a2"));
        int addCount = 0, updateCount = 0;
        for (int i = 0; i < rows.size(); i++) {
            int finalI = i;
            CsvRow csvRow = rows.get(i);
            String name = csvRow.getByName("name");
            Assert.hasText(name, () -> StrUtil.format(I18nMessageUtil.get("i18n.name_field_required.e0c5"), finalI + 1));
            String group = csvRow.getByName("group");
            String address = csvRow.getByName("address");
            Assert.hasText(address, () -> StrUtil.format(I18nMessageUtil.get("i18n.address_field_required.3bc8"), finalI + 1));
            String type = csvRow.getByName("type");
            Assert.hasText(type, () -> StrUtil.format(I18nMessageUtil.get("i18n.type_field_required.7637"), finalI + 1));
            RepositoryModel.RepoType repoType = null;
            if ("Git".equalsIgnoreCase(type)) {
                repoType = RepositoryModel.RepoType.Git;
            } else if ("Svn".equalsIgnoreCase(type)) {
                repoType = RepositoryModel.RepoType.Svn;
            }
            Assert.notNull(repoType, () -> StrUtil.format(I18nMessageUtil.get("i18n.type_field_value_error.14cf"), finalI + 1));
            String protocol = csvRow.getByName("protocol");
            Assert.hasText(protocol, () -> StrUtil.format(I18nMessageUtil.get("i18n.protocol_field_required.7cc2"), finalI + 1));
            GitProtocolEnum gitProtocolEnum = null;
            if ("http".equalsIgnoreCase(protocol) || "https".equalsIgnoreCase(protocol)) {
                gitProtocolEnum = GitProtocolEnum.HTTP;
            } else if ("ssh".equalsIgnoreCase(protocol)) {
                gitProtocolEnum = GitProtocolEnum.SSH;
            }
            Assert.notNull(gitProtocolEnum, () -> StrUtil.format(I18nMessageUtil.get("i18n.protocol_field_value_error.2b41"), finalI + 1));
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
            Assert.state(andUpdateSshKey, StrUtil.format(I18nMessageUtil.get("i18n.rsa_private_key_file_error.b687"), finalI + 1));
            if (where.getRepoType() == RepositoryModel.RepoType.Git.getCode()) {
                // 验证 git 仓库信息
                try {
                    IPlugin plugin = PluginFactory.getPlugin("git-clone");
                    Map<String, Object> map = where.toMap();
                    Tuple branchAndTagList = (Tuple) plugin.execute("branchAndTagList", map);
                    //Tuple tuple = GitUtil.getBranchAndTagList(repositoryModelReq);
                } catch (Exception e) {
                    log.warn(I18nMessageUtil.get("i18n.get_repository_branch_failure.37cc"), e);
                    throw new IllegalStateException(StrUtil.format(I18nMessageUtil.get("i18n.repository_info_error.5b0a"), finalI + 1));
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
        return JsonMessage.success(I18nMessageUtil.get("i18n.import_success_with_count.22b9"), addCount, updateCount);
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
        Assert.notNull(repositoryModel, I18nMessageUtil.get("i18n.no_corresponding_repository.dde9"));
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
        Assert.state(andUpdateSshKey, I18nMessageUtil.get("i18n.rsa_private_key_file_invalid.5f12"));

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
                log.warn(I18nMessageUtil.get("i18n.get_repository_branch_failure.37cc"), e);
                return new JsonMessage<>(500, I18nMessageUtil.get("i18n.unable_to_connect_to_repository.52df") + e.getMessage());
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

        return new JsonMessage<>(200, I18nMessageUtil.get("i18n.operation_succeeded.3313"));
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
        return new JsonMessage<>(200, I18nMessageUtil.get("i18n.operation_succeeded.3313"));
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
        Assert.hasText(token, I18nMessageUtil.get("i18n.please_fill_in_personal_token.970a"));
        // 搜索条件
        // 远程仓库
        //ImportRepoUtil.getProviderConfig(type);

        String userName = ImportRepoUtil.getCurrentUserName(type, token, address);
        cn.hutool.json.JSONObject repoList = ImportRepoUtil.getRepoList(type, condition, page, token, userName, address);
        PageResultDto<JSONObject> pageResultDto = new PageResultDto<>(page.getPageNumber(), page.getPageSize(), repoList.getLong("total").intValue());
        List<JSONObject> objects = repoList.getJSONArray("data")
            .stream()
            .map(o -> {
                cn.hutool.json.JSONObject obj = (cn.hutool.json.JSONObject) o;
                JSONObject jsonObject = new JSONObject();
                jsonObject.putAll(obj);
                jsonObject.put("exists", RepositoryController.this.checkRepositoryUrl(obj.getStr("url"), request));
                return jsonObject;
            })
            .collect(Collectors.toList());
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
        Assert.notNull(repositoryModelReq, I18nMessageUtil.get("i18n.correct_information_required.5e12"));
        Assert.hasText(repositoryModelReq.getName(), I18nMessageUtil.get("i18n.please_fill_in_repository_name.9f0d"));
        Integer repoType = repositoryModelReq.getRepoType();
        Assert.state(repoType != null && (repoType == RepositoryModel.RepoType.Git.getCode() || repoType == RepositoryModel.RepoType.Svn.getCode()), I18nMessageUtil.get("i18n.repository_type_required.9414"));
        Assert.hasText(repositoryModelReq.getGitUrl(), I18nMessageUtil.get("i18n.please_fill_in_repository_address.0cf8"));
        //
        Integer protocol = repositoryModelReq.getProtocol();
        Assert.state(protocol != null && (protocol == GitProtocolEnum.HTTP.getCode() || protocol == GitProtocolEnum.SSH.getCode()), I18nMessageUtil.get("i18n.select_pull_code_protocol.fc24"));
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
        Assert.state(!repositoryUrl, I18nMessageUtil.get("i18n.repo_already_exists.38a3"));
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
            Validator.validateGeneral(id, I18nMessageUtil.get("i18n.wrong_id.ab4d"));
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
        Assert.state(!exists, I18nMessageUtil.get("i18n.current_repository_associated_with_build.4b6e"));
        RepositoryModel keyAndGlobal = repositoryService.getByKeyAndGlobal(id, request);
        repositoryService.delByKey(keyAndGlobal.getId());
        File rsaFile = BuildUtil.getRepositoryRsaFile(id + ServerConst.ID_RSA);
        FileUtil.del(rsaFile);
        return JsonMessage.success(I18nMessageUtil.get("i18n.delete_success.0007"));
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
            return new JsonMessage<>(400, I18nMessageUtil.get("i18n.unsupported_method.a1de") + method);
        }
        return new JsonMessage<>(200, I18nMessageUtil.get("i18n.operation_succeeded.3313"));
    }

}
