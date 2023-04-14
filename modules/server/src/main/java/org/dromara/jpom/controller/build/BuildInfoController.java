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
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.lang.RegexPool;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.plugins.IPlugin;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.build.BuildExecuteService;
import org.dromara.jpom.build.BuildUtil;
import org.dromara.jpom.build.DockerYmlDsl;
import org.dromara.jpom.build.ResultDirFileAction;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.JsonMessage;
import org.dromara.jpom.common.ServerConst;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.common.validator.ValidatorRule;
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
import org.dromara.jpom.system.extconf.BuildExtConfig;
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
    public JsonMessage<PageResultDto<BuildInfoModel>> getBuildList(HttpServletRequest request) {
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
    public JsonMessage<BuildInfoModel> getBuildListAll(String id, HttpServletRequest request) {
        // load list with page
        BuildInfoModel buildInfoModel = buildInfoService.getByKey(id, request);
        Assert.notNull(buildInfoModel, "不存在对应的构建");
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
    public JsonMessage<List<String>> getBuildGroupAll(HttpServletRequest request) {
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
    public JsonMessage<String> updateBuild(String id,
                                           @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "构建名称不能为空") String name,
                                           @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "仓库信息不能为空") String repositoryId,
                                           @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "构建产物目录不能为空,长度1-200", range = "1:200") String resultDirFile,
                                           @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "构建命令不能为空") String script,
                                           @ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "发布方法不正确") int releaseMethod,
                                           String branchName, String branchTagName, String webhook, String autoBuildCron,
                                           String extraData, String group,
                                           @ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "构建方式不正确") int buildMode,
                                           String aliasCode,
                                           HttpServletRequest request) {
        // 根据 repositoryId 查询仓库信息
        RepositoryModel repositoryModel = repositoryService.getByKey(repositoryId, request);
        Assert.notNull(repositoryModel, "无效的仓库信息");
        // 如果是 GIT 需要检测分支是否存在
        if (RepositoryModel.RepoType.Git.getCode() == repositoryModel.getRepoType()) {
            Assert.hasText(branchName, "请选择分支");
        } else if (RepositoryModel.RepoType.Svn.getCode() == repositoryModel.getRepoType()) {
            // 如果是 SVN
            branchName = "trunk";
        }
        ResultDirFileAction resultDirFileAction = ResultDirFileAction.parse(resultDirFile);
        resultDirFileAction.check();
        //
        Assert.state(buildMode == 0 || buildMode == 1, "请选择正确的构建方式");
        if (buildMode == 1) {
            // 验证 dsl 内容
            this.checkDocker(script, request);
            // 容器构建不能使用 ant 模式
            Assert.state(resultDirFileAction.getType() == ResultDirFileAction.Type.ORIGINAL, "容器构建的产物路径不能使用 ant 模式");
        } else {
            if (StrUtil.startWith(script, ServerConst.REF_SCRIPT)) {
                String scriptId = StrUtil.removePrefix(script, ServerConst.REF_SCRIPT);
                ScriptModel keyAndGlobal = scriptServer.getByKeyAndGlobal(scriptId, request, "请选择正确的脚本");
                Assert.notNull(keyAndGlobal, "请选择正确的脚本");
            }
        }
        if (buildExtConfig.isCheckDeleteCommand()) {
            // 判断删除命令
            Assert.state(!CommandUtil.checkContainsDel(script), "不能包含删除命令");
        }
        // 查询构建信息
        BuildInfoModel buildInfoModel = buildInfoService.getByKey(id, request);
        buildInfoModel = ObjectUtil.defaultIfNull(buildInfoModel, new BuildInfoModel());
        // 设置参数
        Opt.ofBlankAble(webhook).ifPresent(s -> Validator.validateMatchRegex(RegexPool.URL_HTTP, s, "WebHooks 地址不合法"));
        Opt.ofBlankAble(aliasCode).ifPresent(s -> Validator.validateGeneral(s, "别名码只能是英文、数字"));
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
        buildInfoModel.setBuildMode(buildMode);
        // 发布方式
        BuildReleaseMethod releaseMethod1 = BaseEnum.getEnum(BuildReleaseMethod.class, releaseMethod);
        Assert.notNull(releaseMethod1, "发布方法不正确");
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
            Assert.hasText(buildInfoModel.getReleaseMethodDataId(), "没有发布分发对应关联数据ID");
        }
        // 验证服务端脚本
        String noticeScriptId = jsonObject.getString("noticeScriptId");
        if (StrUtil.isNotEmpty(noticeScriptId)) {
            ScriptModel scriptModel = scriptServer.getByKey(noticeScriptId, request);
            Assert.notNull(scriptModel, "不存在对应的服务端脚本,请重新选择");
        }
        buildInfoModel.setExtraData(jsonObject.toJSONString());

        // 新增构建信息
        if (StrUtil.isEmpty(id)) {
            // set default buildId
            buildInfoModel.setBuildId(0);
            buildInfoService.insert(buildInfoModel);
            return JsonMessage.success("添加成功", buildInfoModel.getId());
        }

        buildInfoService.updateById(buildInfoModel, request);
        return JsonMessage.success("修改成功", buildInfoModel.getId());
    }

    private void checkDocker(String script, HttpServletRequest request) {
        String workspaceId = buildInfoService.getCheckUserWorkspace(request);
        DockerYmlDsl build = DockerYmlDsl.build(script);
        build.check(dockerInfoService, machineDockerServer, workspaceId);
        //
        String fromTag = build.getFromTag();
        if (StrUtil.isNotEmpty(fromTag)) {
            //
            int count = dockerInfoService.countByTag(workspaceId, fromTag);
            Assert.state(count > 0, fromTag + " 没有找到任何 docker。可能docker tag 填写不正确，需要为 docker 配置标签");
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
        Assert.hasText(releaseMethodDataId, "请选择分发SSH项");

        String releasePath = jsonObject.getString("releasePath");
        Assert.hasText(releasePath, "请输入发布到ssh中的目录");
        releasePath = FileUtil.normalize(releasePath);
        String releaseCommand = jsonObject.getString("releaseCommand");
        List<String> strings = StrUtil.splitTrim(releaseMethodDataId, StrUtil.COMMA);
        for (String releaseMethodDataIdItem : strings) {
            SshModel sshServiceItem = sshService.getByKey(releaseMethodDataIdItem, request);
            Assert.notNull(sshServiceItem, "没有对应的ssh项");
            //
            if (releasePath.startsWith(StrUtil.SLASH)) {
                // 以根路径开始
                List<String> fileDirs = sshServiceItem.fileDirs();
                Assert.notEmpty(fileDirs, sshServiceItem.getName() + "此ssh未授权操作此目录");

                boolean find = false;
                for (String fileDir : fileDirs) {
                    if (FileUtil.isSub(new File(fileDir), new File(releasePath))) {
                        find = true;
                    }
                }
                Assert.state(find, sshServiceItem.getName() + "此ssh未授权操作此目录");
            }
            // 发布命令
            if (StrUtil.isNotEmpty(releaseCommand)) {
                int length = releaseCommand.length();
                Assert.state(length <= 4000, "发布命令长度限制在4000字符");
                //return JsonMessage.getString(405, "请输入发布命令");
                String[] commands = StrUtil.splitToArray(releaseCommand, StrUtil.LF);

                for (String commandItem : commands) {
                    boolean checkInputItem = SshModel.checkInputItem(sshServiceItem, commandItem);
                    Assert.state(checkInputItem, sshServiceItem.getName() + "发布命令中包含禁止执行的命令");
                }
            }
        }
        jsonObject.put("releaseMethodDataId", releaseMethodDataId);
    }

    private String formatDocker(JSONObject jsonObject, HttpServletRequest request) {
        // 发布命令
        String dockerfile = jsonObject.getString("dockerfile");
        Assert.hasText(dockerfile, "请填写要执行的 Dockerfile 路径");
        String fromTag = jsonObject.getString("fromTag");
        if (StrUtil.isNotEmpty(fromTag)) {
            Assert.hasText(fromTag, "请填要执行 docker 标签");
            String workspaceId = dockerInfoService.getCheckUserWorkspace(request);
            int count = dockerInfoService.countByTag(workspaceId, fromTag);
            Assert.state(count > 0, "docker tag 填写不正确,没有找到任何docker");
        }
        String dockerTag = jsonObject.getString("dockerTag");
        Assert.hasText(dockerTag, "请填写镜像标签");
        //
        String dockerSwarmId = jsonObject.getString("dockerSwarmId");
        if (StrUtil.isEmpty(dockerSwarmId)) {
            return "DockerImage";
        }
        String dockerSwarmServiceName = jsonObject.getString("dockerSwarmServiceName");
        Assert.hasText(dockerSwarmServiceName, "请填写集群中的服务名");
        return dockerSwarmId;
    }

    private void formatLocalCommand(JSONObject jsonObject) {
        // 发布命令
        String releaseCommand = jsonObject.getString("releaseCommand");
        if (StrUtil.isNotEmpty(releaseCommand)) {
            int length = releaseCommand.length();
            Assert.state(length <= 4000, "发布命令长度限制在4000字符");
        }
    }

    private void formatOutGiving(JSONObject jsonObject) {
        String releaseMethodDataId = jsonObject.getString("releaseMethodDataId_1");
        Assert.hasText(releaseMethodDataId, "请选择分发项目");
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

        Assert.state(!StrUtil.hasEmpty(releaseMethodDataId2Node, releaseMethodDataId2Project), "请选择节点和项目");
        jsonObject.put("releaseMethodDataId", String.format("%s:%s", releaseMethodDataId2Node, releaseMethodDataId2Project));
        //
        String afterOpt = jsonObject.getString("afterOpt");
        AfterOpt afterOpt1 = BaseEnum.getEnum(AfterOpt.class, Convert.toInt(afterOpt, 0));
        Assert.notNull(afterOpt1, "请选择打包后的操作");
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
        Opt.ofBlankAble(projectSecondaryDirectory).ifPresent(s -> {
            FileUtils.checkSlip(s, e -> new IllegalArgumentException("二级目录不能越级：" + e.getMessage()));
        });
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
    public JsonMessage<JSONObject> branchList(
        @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "仓库ID不能为空") String repositoryId) throws Exception {
        // 根据 repositoryId 查询仓库信息
        RepositoryModel repositoryModel = repositoryService.getByKey(repositoryId, false);
        Assert.notNull(repositoryModel, "无效的仓库信息");
        //
        Assert.state(repositoryModel.getRepoType() == 0, "只有 GIT 仓库才有分支信息");
        IPlugin plugin = PluginFactory.getPlugin("git-clone");
        Map<String, Object> map = repositoryModel.toMap();
        Tuple branchAndTagList = (Tuple) plugin.execute("branchAndTagList", map);
        Assert.notNull(branchAndTagList, "没有任何分支");
        JSONObject jsonObject = new JSONObject();
        List<Object> collection = branchAndTagList.toList();
        jsonObject.put("branch", CollUtil.get(collection, 0));
        jsonObject.put("tags", CollUtil.get(collection, 1));
        return JsonMessage.success("ok", jsonObject);
    }


    /**
     * 删除构建信息
     *
     * @param id 构建ID
     * @return json
     */
    @PostMapping(value = "/build/delete", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public JsonMessage<Object> delete(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据id") String id, HttpServletRequest request) {
        // 查询构建信息
        BuildInfoModel buildInfoModel = buildInfoService.getByKey(id, request);
        Objects.requireNonNull(buildInfoModel, "没有对应数据");
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
        Assert.state(!fastDel, "清理历史构建产物失败,已经重新尝试");
        // 删除构建信息数据
        buildInfoService.delByKey(buildInfoModel.getId(), request);
        return JsonMessage.success("删除成功,并且清理历史构建产物成功");
    }


    /**
     * 清除构建信息
     *
     * @param id 构建ID
     * @return json
     */
    @PostMapping(value = "/build/clean-source", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public JsonMessage<Object> cleanSource(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据id") String id, HttpServletRequest request) {
        // 查询构建信息
        BuildInfoModel buildInfoModel = buildInfoService.getByKey(id, request);
        Objects.requireNonNull(buildInfoModel, "没有对应数据");
        File source = BuildUtil.getSourceById(buildInfoModel.getId());
        // 快速删除
        boolean fastDel = CommandUtil.systemFastDel(source);
        //
        Assert.state(!fastDel, "删除文件失败,请检查");
        return JsonMessage.success("清理成功");
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
    public JsonMessage<String> sortItem(@ValidatorItem String id, @ValidatorItem String method, String compareId, HttpServletRequest request) {
        if (StrUtil.equalsIgnoreCase(method, "top")) {
            buildInfoService.sortToTop(id, request);
        } else if (StrUtil.equalsIgnoreCase(method, "up")) {
            buildInfoService.sortMoveUp(id, compareId, request);
        } else if (StrUtil.equalsIgnoreCase(method, "down")) {
            buildInfoService.sortMoveDown(id, compareId, request);
        } else {
            return new JsonMessage<>(400, "不支持的方式" + method);
        }
        return new JsonMessage<>(200, "操作成功");
    }

}
