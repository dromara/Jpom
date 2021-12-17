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
import cn.hutool.core.lang.RegexPool;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;
import cn.hutool.cron.pattern.CronPattern;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorConfig;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.jpom.build.BuildUtil;
import io.jpom.common.BaseServerController;
import io.jpom.model.AfterOpt;
import io.jpom.model.BaseEnum;
import io.jpom.model.PageResultDto;
import io.jpom.model.data.BuildInfoModel;
import io.jpom.model.data.RepositoryModel;
import io.jpom.model.data.SshModel;
import io.jpom.model.enums.BuildReleaseMethod;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.dblog.BuildInfoService;
import io.jpom.service.dblog.DbBuildHistoryLogService;
import io.jpom.service.dblog.RepositoryService;
import io.jpom.service.node.ssh.SshService;
import io.jpom.system.ServerExtConfigBean;
import io.jpom.util.CommandUtil;
import io.jpom.util.GitUtil;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.List;
import java.util.Objects;

/**
 * 构建列表，新版本，数据存放到数据库，不再是文件了
 * 以前的数据会在程序启动时插入到数据库中
 *
 * @author Hotstrip
 * @date 2021-08-09
 */
@RestController
@Feature(cls = ClassFeature.BUILD)
public class BuildInfoController extends BaseServerController {


	private final DbBuildHistoryLogService dbBuildHistoryLogService;
	private final SshService sshService;
	private final BuildInfoService buildInfoService;
	private final RepositoryService repositoryService;

	public BuildInfoController(DbBuildHistoryLogService dbBuildHistoryLogService,
							   SshService sshService,
							   BuildInfoService buildInfoService,
							   RepositoryService repositoryService) {
		this.dbBuildHistoryLogService = dbBuildHistoryLogService;
		this.sshService = sshService;
		this.buildInfoService = buildInfoService;
		this.repositoryService = repositoryService;
	}

	/**
	 * load build list with params
	 *
	 * @return json
	 */
	@RequestMapping(value = "/build/list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.LIST)
	public String getBuildList() {
		// load list with page
		PageResultDto<BuildInfoModel> list = buildInfoService.listPage(getRequest());
		return JsonMessage.getString(200, "", list);
	}

	/**
	 * load build list with params
	 *
	 * @return json
	 */
	@GetMapping(value = "/build/list_all", produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.LIST)
	public String getBuildListAll() {
		// load list with page
		List<BuildInfoModel> modelList = buildInfoService.listByWorkspace(getRequest());
		return JsonMessage.getString(200, "", modelList);
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
	public String updateMonitor(String id,
								@ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "构建名称不能为空")) String name,
								@ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "仓库信息不能为空")) String repositoryId,
								@ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "构建产物目录不能为空,长度1-200", range = "1:200")) String resultDirFile,
								@ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "构建命令不能为空")) String script,
								@ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "发布方法不正确") int releaseMethod,
								String branchName, String branchTagName, String webhook, String autoBuildCron,
								String extraData) {
		// 根据 repositoryId 查询仓库信息
		RepositoryModel repositoryModel = repositoryService.getByKey(repositoryId, getRequest());
		Assert.notNull(repositoryModel, "无效的仓库信息");
		// 如果是 GIT 需要检测分支是否存在
		if (RepositoryModel.RepoType.Git.getCode() == repositoryModel.getRepoType()) {
			Assert.hasText(branchName, "请选择分支");
		} else if (RepositoryModel.RepoType.Svn.getCode() == repositoryModel.getRepoType()) {
			// 如果是 SVN
			branchName = "trunk";
		}
		if (ServerExtConfigBean.getInstance().getBuildCheckDeleteCommand()) {
			// 判断删除命令
			Assert.state(!CommandUtil.checkContainsDel(script), "不能包含删除命令");
		}
		// 查询构建信息
		BuildInfoModel buildInfoModel = buildInfoService.getByKey(id, getRequest());
		if (null == buildInfoModel) {
			buildInfoModel = new BuildInfoModel();
		}
		// 设置参数
		if (StrUtil.isNotEmpty(webhook)) {
			Validator.validateMatchRegex(RegexPool.URL_HTTP, webhook, "WebHooks 地址不合法");
		}
		if (StrUtil.isNotEmpty(autoBuildCron)) {
			try {
				new CronPattern(autoBuildCron);
			} catch (Exception e) {
				throw new IllegalArgumentException("定时构建表达式格式不正确");
			}
		}
		buildInfoModel.setAutoBuildCron(autoBuildCron);
		buildInfoModel.setWebhook(webhook);
		buildInfoModel.setRepositoryId(repositoryId);
		buildInfoModel.setName(name);
		buildInfoModel.setBranchName(branchName);
		buildInfoModel.setBranchTagName(branchTagName);
		buildInfoModel.setResultDirFile(resultDirFile);
		buildInfoModel.setScript(script);
		// 设置修改人
		//buildInfoModel.setModifyUser(UserModel.getOptUserName(getUser()));
		// 发布方式
		BuildReleaseMethod releaseMethod1 = BaseEnum.getEnum(BuildReleaseMethod.class, releaseMethod);
		Assert.notNull(releaseMethod1, "发布方法不正确");
		buildInfoModel.setReleaseMethod(releaseMethod1.getCode());
		// 把 extraData 信息转换成 JSON 字符串
		JSONObject jsonObject = JSON.parseObject(extraData);

		// 验证发布方式 和 extraData 信息
		if (releaseMethod1 == BuildReleaseMethod.Project) {
			this.formatProject(jsonObject);
		} else if (releaseMethod1 == BuildReleaseMethod.Ssh) {
			this.formatSsh(jsonObject);
		} else if (releaseMethod1 == BuildReleaseMethod.Outgiving) {
			String releaseMethodDataId = jsonObject.getString("releaseMethodDataId_1");
			Assert.hasText(releaseMethodDataId, "请选择分发项目");
			jsonObject.put("releaseMethodDataId", releaseMethodDataId);
		} else if (releaseMethod1 == BuildReleaseMethod.LocalCommand) {
			this.formatLocalCommand(jsonObject);
			jsonObject.put("releaseMethodDataId", "LocalCommand");
		}
		// 检查关联数据ID
		buildInfoModel.setReleaseMethodDataId(jsonObject.getString("releaseMethodDataId"));
		if (buildInfoModel.getReleaseMethod() != BuildReleaseMethod.No.getCode()) {
			Assert.hasText(buildInfoModel.getReleaseMethodDataId(), "没有发布分发对应关联数据ID");
		}
		buildInfoModel.setExtraData(jsonObject.toJSONString());

		// 新增构建信息
		if (StrUtil.isEmpty(id)) {
			// set default buildId
			buildInfoModel.setBuildId(0);
			buildInfoService.insert(buildInfoModel);
			return JsonMessage.getString(200, "添加成功");
		}

		buildInfoService.update(buildInfoModel);
		return JsonMessage.getString(200, "修改成功");
	}

	/**
	 * 验证构建信息
	 * 当发布方式为【SSH】的时候
	 *
	 * @param jsonObject 配置信息
	 */
	private void formatSsh(JSONObject jsonObject) {
		// 发布方式
		String releaseMethodDataId = jsonObject.getString("releaseMethodDataId_3");
		Assert.hasText(releaseMethodDataId, "请选择分发SSH项");

		String releasePath = jsonObject.getString("releasePath");
		Assert.hasText(releasePath, "请输入发布到ssh中的目录");

		releasePath = FileUtil.normalize(releasePath);
		SshModel sshServiceItem = sshService.getByKey(releaseMethodDataId, getRequest());
		Assert.notNull(sshServiceItem, "没有对应的ssh项");
		jsonObject.put("releaseMethodDataId", releaseMethodDataId);
		//
		if (releasePath.startsWith(StrUtil.SLASH)) {
			// 以根路径开始
			List<String> fileDirs = sshServiceItem.fileDirs();
			Assert.notEmpty(fileDirs, "此ssh未授权操作此目录");

			boolean find = false;
			for (String fileDir : fileDirs) {
				if (FileUtil.isSub(new File(fileDir), new File(releasePath))) {
					find = true;
				}
			}
			Assert.state(find, "此ssh未授权操作此目录");
		}
		// 发布命令
		String releaseCommand = jsonObject.getString("releaseCommand");
		if (StrUtil.isNotEmpty(releaseCommand)) {
			int length = releaseCommand.length();
			Assert.state(length <= 4000, "发布命令长度限制在4000字符");
			//return JsonMessage.getString(405, "请输入发布命令");
			String[] commands = StrUtil.splitToArray(releaseCommand, StrUtil.LF);

			for (String commandItem : commands) {
				boolean checkInputItem = SshModel.checkInputItem(sshServiceItem, commandItem);
				Assert.state(checkInputItem, "发布命令中包含禁止执行的命令");
			}
		}
	}

	private void formatLocalCommand(JSONObject jsonObject) {
		// 发布命令
		String releaseCommand = jsonObject.getString("releaseCommand");
		if (StrUtil.isNotEmpty(releaseCommand)) {
			int length = releaseCommand.length();
			Assert.state(length <= 4000, "发布命令长度限制在4000字符");
		}
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
	public String branchList(
			@ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "仓库ID不能为空")) String repositoryId) throws Exception {
		// 根据 repositoryId 查询仓库信息
		RepositoryModel repositoryModel = repositoryService.getByKey(repositoryId, false);
		Assert.notNull(repositoryModel, "无效的仓库信息");
		//
		Assert.state(repositoryModel.getRepoType() == 0, "只有 GIT 仓库才有分支信息");
		Tuple branchAndTagList = GitUtil.getBranchAndTagList(repositoryModel);
		Assert.notNull(branchAndTagList, "没有任何分支");
		Object[] members = branchAndTagList.getMembers();
		return JsonMessage.getString(200, "ok", members);
	}


	/**
	 * 删除构建信息
	 *
	 * @param id 构建ID
	 * @return json
	 */
	@PostMapping(value = "/build/delete", produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.DEL)
	public String delete(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据id") String id) {
		// 查询构建信息
		BuildInfoModel buildInfoModel = buildInfoService.getByKey(id, getRequest());
		Objects.requireNonNull(buildInfoModel, "没有对应数据");
		//
		String e = buildInfoService.checkStatus(buildInfoModel.getStatus());
		Assert.isNull(e, () -> e);
		dbBuildHistoryLogService.delByBuildId(buildInfoModel.getId());

		// 删除构建信息文件
		File file = BuildUtil.getBuildDataFile(buildInfoModel.getId());
		if (!FileUtil.del(file)) {
			FileUtil.del(file.toPath());
			return JsonMessage.getString(500, "清理历史构建产物失败,已经重新尝试");
		}

		// 删除构建信息数据
		buildInfoService.delByKey(buildInfoModel.getId());
		return JsonMessage.getString(200, "清理历史构建产物成功");
	}


	/**
	 * 清除构建信息
	 *
	 * @param id 构建ID
	 * @return json
	 */
	@PostMapping(value = "/build/clean-source", produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.EXECUTE)
	public String cleanSource(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据id") String id) {
		// 查询构建信息
		BuildInfoModel buildInfoModel = buildInfoService.getByKey(id, getRequest());
		Objects.requireNonNull(buildInfoModel, "没有对应数据");

		File source = BuildUtil.getSourceById(buildInfoModel.getId());
		boolean del = FileUtil.del(source);
		if (!del) {
			del = FileUtil.del(source.toPath());
		}
		return JsonMessage.getString(200, "清理成功", del);
	}

}
