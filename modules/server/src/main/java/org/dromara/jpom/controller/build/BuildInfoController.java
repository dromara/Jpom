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
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.lang.RegexPool;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import cn.keepbx.jpom.plugins.IPlugin;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.IDockerConfigPlugin;
import org.dromara.jpom.build.BuildExecuteService;
import org.dromara.jpom.build.BuildUtil;
import org.dromara.jpom.build.DockerYmlDsl;
import org.dromara.jpom.build.ResultDirFileAction;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.ServerConst;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.common.validator.ValidatorRule;
import org.dromara.jpom.configuration.BuildExtConfig;
import org.dromara.jpom.func.assets.server.MachineDockerServer;
import org.dromara.jpom.model.AfterOpt;
import org.dromara.jpom.model.BaseEnum;
import org.dromara.jpom.model.PageResultDto;
import org.dromara.jpom.model.data.BuildInfoModel;
import org.dromara.jpom.model.data.RepositoryModel;
import org.dromara.jpom.model.data.SshModel;
import org.dromara.jpom.model.enums.BuildReleaseMethod;
import org.dromara.jpom.model.script.ScriptModel;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.plugin.PluginFactory;
import org.dromara.jpom.service.dblog.BuildInfoService;
import org.dromara.jpom.service.dblog.DbBuildHistoryLogService;
import org.dromara.jpom.service.dblog.RepositoryService;
import org.dromara.jpom.service.docker.DockerInfoService;
import org.dromara.jpom.service.node.ssh.SshService;
import org.dromara.jpom.service.script.ScriptServer;
import org.dromara.jpom.util.CommandUtil;
import org.dromara.jpom.util.FileUtils;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 构建列表，新版本，数据存放到数据库，不再是文件了
 * 以前的数据会在程序启动时插入到数据库中
 *
 * @author Hotstrip
 * @since 2021-08-09
 */
@RestController
@Feature(cls = ClassFeature.BUILD)
public class BuildInfoController extends BaseServerController {

    private final DbBuildHistoryLogService dbBuildHistoryLogService;
    private final SshService sshService;
    private final BuildInfoService buildInfoService;
    private final RepositoryService repositoryService;
    private final BuildExecuteService buildExecuteService;
    private final DockerInfoService dockerInfoService;
    private final ScriptServer scriptServer;
    private final BuildExtConfig buildExtConfig;
    protected final MachineDockerServer machineDockerServer;

    public BuildInfoController(DbBuildHistoryLogService dbBuildHistoryLogService,
                               SshService sshService,
                               BuildInfoService buildInfoService,
                               RepositoryService repositoryService,
                               BuildExecuteService buildExecuteService,
                               DockerInfoService dockerInfoService,
                               ScriptServer scriptServer,
                               BuildExtConfig buildExtConfig,
                               MachineDockerServer machineDockerServer) {
        this.dbBuildHistoryLogService = dbBuildHistoryLogService;
        this.sshService = sshService;
        this.buildInfoService = buildInfoService;
        this.repositoryService = repositoryService;
        this.buildExecuteService = buildExecuteService;
        this.dockerInfoService = dockerInfoService;
        this.scriptServer = scriptServer;
        this.buildExtConfig = buildExtConfig;
        this.machineDockerServer = machineDockerServer;
    }

    /**
     * load build list with params
     *
     * @return json
     */
    @RequestMapping(value = "/build/list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<PageResultDto<BuildInfoModel>> getBuildList(HttpServletRequest request) {
        // load list with page
        PageResultDto<BuildInfoModel> page = buildInfoService.listPage(request);
        page.each(buildInfoModel -> {
            // 获取源码目录是否存在
            File source = BuildUtil.getSourceById(buildInfoModel.getId());
            buildInfoModel.setSourceDirExist(FileUtil.exist(source));
            //
            File file = BuildUtil.getHistoryPackageFile(buildInfoModel.getId(), buildInfoModel.getBuildId(), buildInfoModel.getResultDirFile());
            buildInfoModel.setResultHasFile(FileUtil.exist(file));
        });
        return JsonMessage.success("", page);
    }

    /**
     * load build list with params
     *
     * @return json
     */
    @GetMapping(value = "/build/get", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<BuildInfoModel> getBuildListAll(String id, HttpServletRequest request) {
        // load list with page
        BuildInfoModel buildInfoModel = buildInfoService.getByKey(id, request);
        Assert.notNull(buildInfoModel, I18nMessageUtil.get("i18n.build_not_exist.c2ac"));
        // 获取源码目录是否存在
        File source = BuildUtil.getSourceById(buildInfoModel.getId());
        buildInfoModel.setSourceDirExist(FileUtil.exist(source));
        //
        File file = BuildUtil.getHistoryPackageFile(buildInfoModel.getId(), buildInfoModel.getBuildId(), buildInfoModel.getResultDirFile());
        buildInfoModel.setResultHasFile(FileUtil.exist(file));
        return JsonMessage.success("", buildInfoModel);
    }

    /**
     * load build list with params
     *
     * @return json
     */
    @GetMapping(value = "/build/list_group_all", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<List<String>> getBuildGroupAll(HttpServletRequest request) {
        // load list with page
        List<String> group = buildInfoService.listGroup(request);
        return JsonMessage.success("", group);
    }

    /**
     * edit build info
     *
     * @param id            构建ID
     * @param name          构建名称
     * @param repositoryId  仓库ID
     * @param resultDirFile 构建产物目录
     * @param script        构建命令
     * @param releaseMethod 发布方法
     * @param branchName    分支名称
     * @param webhook       webhook
     * @param extraData     构建的其他信息
     * @param autoBuildCron 自动构建表达是
     * @param branchTagName 标签名
     * @return json
     */
    @RequestMapping(value = "/build/edit", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<String> updateBuild(String id,
                                            @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.build_name_not_empty.4154") String name,
                                            @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.repository_info_cannot_be_empty.67d2") String repositoryId,
                                            @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.build_product_dir_not_empty.ba06", range = "1:200") String resultDirFile,
                                            @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.build_command_not_empty.2e37") String script,
                                            @ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "i18n.incorrect_publish_method.e095") int releaseMethod,
                                            String branchName, String branchTagName, String webhook, String autoBuildCron,
                                            String extraData, String group,
                                            @ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "i18n.build_method_incorrect.5319") int buildMode,
                                            String aliasCode,
                                            @ValidatorItem(value = ValidatorRule.NUMBERS, msg = "i18n.correct_retention_days_required.d542") Integer resultKeepDay,
                                            String buildEnvParameter,
                                            HttpServletRequest request) {
        // 根据 repositoryId 查询仓库信息
        RepositoryModel repositoryModel = repositoryService.getByKey(repositoryId, request);
        Assert.notNull(repositoryModel, I18nMessageUtil.get("i18n.invalid_repository_info.b4ad"));
        // 如果是 GIT 需要检测分支是否存在
        if (RepositoryModel.RepoType.Git.getCode() == repositoryModel.getRepoType()) {
            Assert.hasText(branchName, I18nMessageUtil.get("i18n.branch_required.5095"));
        } else if (RepositoryModel.RepoType.Svn.getCode() == repositoryModel.getRepoType()) {
            // 如果是 SVN
            branchName = "trunk";
        }
        ResultDirFileAction resultDirFileAction = ResultDirFileAction.parse(resultDirFile);
        resultDirFileAction.check();
        //
        Assert.state(buildMode == 0 || buildMode == 1, I18nMessageUtil.get("i18n.select_correct_build_method.84c4"));
        if (buildMode == 1) {
            // 验证 dsl 内容
            this.checkDocker(script, request);
            // 容器构建不能使用 ant 模式
            Assert.state(resultDirFileAction.getType() == ResultDirFileAction.Type.ORIGINAL, I18nMessageUtil.get("i18n.container_build_product_path_cannot_use_ant_pattern.ddc7"));
        } else {
            if (StrUtil.startWith(script, ServerConst.REF_SCRIPT)) {
                String scriptId = StrUtil.removePrefix(script, ServerConst.REF_SCRIPT);
                ScriptModel keyAndGlobal = scriptServer.getByKeyAndGlobal(scriptId, request, I18nMessageUtil.get("i18n.select_correct_script.ff2d"));
                Assert.notNull(keyAndGlobal, I18nMessageUtil.get("i18n.select_correct_script.ff2d"));
            }
        }
        if (buildExtConfig.isCheckDeleteCommand()) {
            // 判断删除命令
            Assert.state(!CommandUtil.checkContainsDel(script), I18nMessageUtil.get("i18n.build_command_no_delete.df52"));
        }
        // 查询构建信息
        BuildInfoModel buildInfoModel = buildInfoService.getByKey(id, request);
        buildInfoModel = ObjectUtil.defaultIfNull(buildInfoModel, new BuildInfoModel());
        // 设置参数
        Opt.ofBlankAble(webhook).ifPresent(s -> Validator.validateMatchRegex(RegexPool.URL_HTTP, s, I18nMessageUtil.get("i18n.invalid_webhooks_address.d836")));
        Opt.ofBlankAble(aliasCode).ifPresent(s -> Validator.validateGeneral(s, I18nMessageUtil.get("i18n.alias_code_validation.8b99")));
        //
        buildInfoModel.setAutoBuildCron(this.checkCron(autoBuildCron));
        buildInfoModel.setWebhook(webhook);
        buildInfoModel.setRepositoryId(repositoryId);
        buildInfoModel.setName(name);
        buildInfoModel.setAliasCode(aliasCode);
        buildInfoModel.setBranchName(branchName);
        buildInfoModel.setBranchTagName(branchTagName);
        buildInfoModel.setResultDirFile(resultDirFile);
        buildInfoModel.setScript(script);
        buildInfoModel.setGroup(group);
        buildInfoModel.setResultKeepDay(resultKeepDay);
        buildInfoModel.setBuildMode(buildMode);
        buildInfoModel.setBuildEnvParameter(buildEnvParameter);
        // 发布方式
        BuildReleaseMethod releaseMethod1 = BaseEnum.getEnum(BuildReleaseMethod.class, releaseMethod);
        Assert.notNull(releaseMethod1, I18nMessageUtil.get("i18n.incorrect_publish_method.e095"));
        buildInfoModel.setReleaseMethod(releaseMethod1.getCode());
        // 把 extraData 信息转换成 JSON 字符串 ,不能直接使用 io.jpom.build.BuildExtraModule
        JSONObject jsonObject = JSON.parseObject(extraData);

        // 验证发布方式 和 extraData 信息
        if (releaseMethod1 == BuildReleaseMethod.Project) {
            this.formatProject(jsonObject);
        } else if (releaseMethod1 == BuildReleaseMethod.Ssh) {
            this.formatSsh(jsonObject, request);
        } else if (releaseMethod1 == BuildReleaseMethod.Outgiving) {
            this.formatOutGiving(jsonObject);
        } else if (releaseMethod1 == BuildReleaseMethod.LocalCommand) {
            this.formatLocalCommand(jsonObject);
            jsonObject.put("releaseMethodDataId", "LocalCommand");
        } else if (releaseMethod1 == BuildReleaseMethod.DockerImage) {
            // dockerSwarmId default
            String dockerSwarmId = this.formatDocker(jsonObject, request);
            jsonObject.put("releaseMethodDataId", dockerSwarmId);
        }
        // 检查关联数据ID
        buildInfoModel.setReleaseMethodDataId(jsonObject.getString("releaseMethodDataId"));
        if (buildInfoModel.getReleaseMethod() != BuildReleaseMethod.No.getCode()) {
            Assert.hasText(buildInfoModel.getReleaseMethodDataId(), I18nMessageUtil.get("i18n.no_publish_distribution_related_data_id.a077"));
        }
        // 验证服务端脚本
        String noticeScriptId = jsonObject.getString("noticeScriptId");
        if (StrUtil.isNotEmpty(noticeScriptId)) {
            List<String> list = StrUtil.splitTrim(noticeScriptId, StrUtil.COMMA);
            for (String noticeScriptIdItem : list) {
                ScriptModel scriptModel = scriptServer.getByKey(noticeScriptIdItem, request);
                Assert.notNull(scriptModel, I18nMessageUtil.get("i18n.server_script_not_exist.de24"));
            }
        }
        buildInfoModel.setExtraData(jsonObject.toJSONString());

        // 新增构建信息
        if (StrUtil.isEmpty(id)) {
            // set default buildId
            buildInfoModel.setBuildId(0);
            buildInfoService.insert(buildInfoModel);
            return JsonMessage.success(I18nMessageUtil.get("i18n.addition_succeeded.3fda"), buildInfoModel.getId());
        }

        buildInfoService.updateById(buildInfoModel, request);
        return JsonMessage.success(I18nMessageUtil.get("i18n.modify_success.69be"), buildInfoModel.getId());
    }

    private void checkDocker(String script, HttpServletRequest request) {
        String workspaceId = buildInfoService.getCheckUserWorkspace(request);
        DockerYmlDsl build = DockerYmlDsl.build(script);
        //
        IDockerConfigPlugin plugin = (IDockerConfigPlugin) PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
        build.check(dockerInfoService, machineDockerServer, workspaceId, plugin);
        //
        String fromTag = build.getFromTag();
        if (StrUtil.isNotEmpty(fromTag)) {
            //
            int count = dockerInfoService.countByTag(workspaceId, fromTag);
            Assert.state(count > 0, fromTag + I18nMessageUtil.get("i18n.docker_not_found.2a2e"));
        }
    }

    /**
     * 验证构建信息
     * 当发布方式为【SSH】的时候
     *
     * @param jsonObject 配置信息
     */
    private void formatSsh(JSONObject jsonObject, HttpServletRequest request) {
        // 发布方式
        String releaseMethodDataId = jsonObject.getString("releaseMethodDataId_3");
        Assert.hasText(releaseMethodDataId, I18nMessageUtil.get("i18n.ssh_item_distribution_required.2884"));

        String releasePath = jsonObject.getString("releasePath");
        Assert.hasText(releasePath, I18nMessageUtil.get("i18n.publish_to_ssh_directory_required.56a6"));
        releasePath = FileUtil.normalize(releasePath);
        String releaseCommand = jsonObject.getString("releaseCommand");
        List<String> strings = StrUtil.splitTrim(releaseMethodDataId, StrUtil.COMMA);
        for (String releaseMethodDataIdItem : strings) {
            SshModel sshServiceItem = sshService.getByKey(releaseMethodDataIdItem, request);
            Assert.notNull(sshServiceItem, I18nMessageUtil.get("i18n.no_corresponding_ssh_item.2deb"));
            //
            if (releasePath.startsWith(StrUtil.SLASH)) {
                // 以根路径开始
                List<String> fileDirs = sshServiceItem.fileDirs();
                Assert.notEmpty(fileDirs, sshServiceItem.getName() + I18nMessageUtil.get("i18n.ssh_unauthorized_directory.df78"));

                boolean find = false;
                for (String fileDir : fileDirs) {
                    if (FileUtil.isSub(new File(fileDir), new File(releasePath))) {
                        find = true;
                    }
                }
                Assert.state(find, sshServiceItem.getName() + I18nMessageUtil.get("i18n.ssh_unauthorized_directory.df78"));
            }
            // 发布命令
            if (StrUtil.isNotEmpty(releaseCommand)) {
                int length = releaseCommand.length();
                Assert.state(length <= 4000, I18nMessageUtil.get("i18n.publish_command_length_limit.66b0"));
                //return JsonMessage.getString(405, "请输入发布命令");
                String[] commands = StrUtil.splitToArray(releaseCommand, StrUtil.LF);

                for (String commandItem : commands) {
                    boolean checkInputItem = SshModel.checkInputItem(sshServiceItem, commandItem);
                    Assert.state(checkInputItem, sshServiceItem.getName() + I18nMessageUtil.get("i18n.publish_command_contains_forbidden_command.097d"));
                }
            }
        }
        jsonObject.put("releaseMethodDataId", releaseMethodDataId);
    }

    private String formatDocker(JSONObject jsonObject, HttpServletRequest request) {
        // 发布命令
        String dockerfile = jsonObject.getString("dockerfile");
        Assert.hasText(dockerfile, I18nMessageUtil.get("i18n.dockerfile_path_required.69ac"));
        String fromTag = jsonObject.getString("fromTag");
        if (StrUtil.isNotEmpty(fromTag)) {
            Assert.hasText(fromTag, I18nMessageUtil.get("i18n.docker_label_required.b690"));
            String workspaceId = dockerInfoService.getCheckUserWorkspace(request);
            int count = dockerInfoService.countByTag(workspaceId, fromTag);
            Assert.state(count > 0, I18nMessageUtil.get("i18n.docker_tag_incorrect.8b62"));
        }
        String dockerTag = jsonObject.getString("dockerTag");
        Assert.hasText(dockerTag, I18nMessageUtil.get("i18n.image_tag_required.92cf"));
        //
        String dockerSwarmId = jsonObject.getString("dockerSwarmId");
        if (StrUtil.isEmpty(dockerSwarmId)) {
            return "DockerImage";
        }
        String dockerSwarmServiceName = jsonObject.getString("dockerSwarmServiceName");
        Assert.hasText(dockerSwarmServiceName, I18nMessageUtil.get("i18n.service_name_in_cluster_required.5446"));
        return dockerSwarmId;
    }

    private void formatLocalCommand(JSONObject jsonObject) {
        // 发布命令
        String releaseCommand = jsonObject.getString("releaseCommand");
        if (StrUtil.isNotEmpty(releaseCommand)) {
            int length = releaseCommand.length();
            Assert.state(length <= 4000, I18nMessageUtil.get("i18n.publish_command_length_limit.66b0"));
        }
    }

    private void formatOutGiving(JSONObject jsonObject) {
        String releaseMethodDataId = jsonObject.getString("releaseMethodDataId_1");
        Assert.hasText(releaseMethodDataId, I18nMessageUtil.get("i18n.distribution_project_required.2560"));
        jsonObject.put("releaseMethodDataId", releaseMethodDataId);
        //
        this.checkProjectSecondaryDirectory(jsonObject);
    }

    /**
     * 验证构建信息
     * 当发布方式为【项目】的时候
     *
     * @param jsonObject 配置信息
     */
    private void formatProject(JSONObject jsonObject) {
        String releaseMethodDataId2Node = jsonObject.getString("releaseMethodDataId_2_node");
        String releaseMethodDataId2Project = jsonObject.getString("releaseMethodDataId_2_project");

        Assert.state(!StrUtil.hasEmpty(releaseMethodDataId2Node, releaseMethodDataId2Project), I18nMessageUtil.get("i18n.select_node_and_project.6021"));
        jsonObject.put("releaseMethodDataId", String.format("%s:%s", releaseMethodDataId2Node, releaseMethodDataId2Project));
        //
        String afterOpt = jsonObject.getString("afterOpt");
        AfterOpt afterOpt1 = BaseEnum.getEnum(AfterOpt.class, Convert.toInt(afterOpt, 0));
        Assert.notNull(afterOpt1, I18nMessageUtil.get("i18n.post_packaging_action_required.bf66"));
        //
        String clearOld = jsonObject.getString("clearOld");
        jsonObject.put("afterOpt", afterOpt1.getCode());
        jsonObject.put("clearOld", Convert.toBool(clearOld, false));
        //
        this.checkProjectSecondaryDirectory(jsonObject);
    }

    private void checkProjectSecondaryDirectory(JSONObject jsonObject) {
        //
        String projectSecondaryDirectory = jsonObject.getString("projectSecondaryDirectory");
        Opt.ofBlankAble(projectSecondaryDirectory).ifPresent(s -> FileUtils.checkSlip(s, e -> new IllegalArgumentException(I18nMessageUtil.get("i18n.second_level_directory_cannot_skip_levels.c9fb") + e.getMessage())));
    }

    /**
     * 获取分支信息
     *
     * @param repositoryId 仓库id
     * @return json
     * @throws Exception 异常
     */
    @RequestMapping(value = "/build/branch-list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<JSONObject> branchList(
        @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.repository_id_cannot_be_empty.a42c") String repositoryId) throws Exception {
        // 根据 repositoryId 查询仓库信息
        RepositoryModel repositoryModel = repositoryService.getByKey(repositoryId, false);
        Assert.notNull(repositoryModel, I18nMessageUtil.get("i18n.invalid_repository_info.b4ad"));
        //
        Assert.state(repositoryModel.getRepoType() == 0, I18nMessageUtil.get("i18n.only_git_repositories_have_branch_info.d7f7"));
        IPlugin plugin = PluginFactory.getPlugin("git-clone");
        Map<String, Object> map = repositoryModel.toMap();
        Tuple branchAndTagList = (Tuple) plugin.execute("branchAndTagList", map);
        Assert.notNull(branchAndTagList, I18nMessageUtil.get("i18n.no_any_branch.d042"));
        JSONObject jsonObject = new JSONObject();
        List<Object> collection = branchAndTagList.toList();
        jsonObject.put("branch", CollUtil.get(collection, 0));
        jsonObject.put("tags", CollUtil.get(collection, 1));
        return JsonMessage.success("", jsonObject);
    }


    /**
     * 删除构建信息
     *
     * @param id 构建ID
     * @return json
     */
    @PostMapping(value = "/build/delete", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public IJsonMessage<Object> delete(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.data_id_not_found.1b0a") String id, HttpServletRequest request) {
        this.delById(id, request);
        return JsonMessage.success(I18nMessageUtil.get("i18n.delete_success_with_cleanup.6155"));
    }


    private void delById(String id, HttpServletRequest request) {
        // 查询构建信息
        BuildInfoModel buildInfoModel = buildInfoService.getByKey(id, request);
        Objects.requireNonNull(buildInfoModel, I18nMessageUtil.get("i18n.no_data_found.4ffb"));
        //
        String e = buildExecuteService.checkStatus(buildInfoModel);
        Assert.isNull(e, () -> e);
        // 删除构建历史
        dbBuildHistoryLogService.delByWorkspace(request, entity -> entity.set("buildDataId", buildInfoModel.getId()));
        // 删除构建信息文件
        File file = BuildUtil.getBuildDataFile(buildInfoModel.getId());
        // 快速删除
        boolean fastDel = CommandUtil.systemFastDel(file);
        //
        Assert.state(!fastDel, I18nMessageUtil.get("i18n.cleanup_history_build_failed_retrying.088e"));
        // 删除构建信息数据
        buildInfoService.delByKey(buildInfoModel.getId(), request);
    }

    /**
     * 批量删除构建信息
     *
     * @param ids 构建ID
     * @return json
     */
    @PostMapping(value = "/build/batch-delete", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public IJsonMessage<Object> batchDelete(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.data_id_not_found.1b0a") String ids, HttpServletRequest request) {
        List<String> list = StrUtil.splitTrim(ids, StrUtil.COMMA);
        for (String s : list) {
            this.delById(s, request);
        }
        return JsonMessage.success(I18nMessageUtil.get("i18n.delete_success_with_cleanup.6155"));
    }


    /**
     * 清除构建信息
     *
     * @param id 构建ID
     * @return json
     */
    @PostMapping(value = "/build/clean-source", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public IJsonMessage<Object> cleanSource(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.data_id_not_found.1b0a") String id, HttpServletRequest request) {
        // 查询构建信息
        BuildInfoModel buildInfoModel = buildInfoService.getByKey(id, request);
        Objects.requireNonNull(buildInfoModel, I18nMessageUtil.get("i18n.no_data_found.4ffb"));
        File source = BuildUtil.getSourceById(buildInfoModel.getId());
        // 快速删除
        boolean fastDel = CommandUtil.systemFastDel(source);
        //
        Assert.state(!fastDel, I18nMessageUtil.get("i18n.delete_file_failure.041f"));
        return JsonMessage.success(I18nMessageUtil.get("i18n.cleanup_succeeded.02ea"));
    }

    /**
     * 排序
     *
     * @param id        节点ID
     * @param method    方法
     * @param compareId 比较的ID
     * @return msg
     */
    @GetMapping(value = "/build/sort-item", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<String> sortItem(@ValidatorItem String id, @ValidatorItem String method, String compareId, HttpServletRequest request) {
        if (StrUtil.equalsIgnoreCase(method, "top")) {
            buildInfoService.sortToTop(id, request);
        } else if (StrUtil.equalsIgnoreCase(method, "up")) {
            buildInfoService.sortMoveUp(id, compareId, request);
        } else if (StrUtil.equalsIgnoreCase(method, "down")) {
            buildInfoService.sortMoveDown(id, compareId, request);
        } else {
            return new JsonMessage<>(400, I18nMessageUtil.get("i18n.unsupported_method.a1de") + method);
        }
        return new JsonMessage<>(200, I18nMessageUtil.get("i18n.operation_succeeded.3313"));
    }

}
