package io.jpom.controller.build;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.Page;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorConfig;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.jpom.build.BuildUtil;
import io.jpom.common.BaseServerController;
import io.jpom.common.Const;
import io.jpom.common.interceptor.OptLog;
import io.jpom.model.AfterOpt;
import io.jpom.model.BaseEnum;
import io.jpom.model.data.*;
import io.jpom.model.log.UserOperateLogV1;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.build.BuildService;
import io.jpom.service.dblog.BuildInfoService;
import io.jpom.service.dblog.DbBuildHistoryLogService;
import io.jpom.service.dblog.RepositoryService;
import io.jpom.service.node.ssh.SshService;
import io.jpom.util.CommandUtil;
import io.jpom.util.GitUtil;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
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

	@Resource
	private BuildService buildService;
	@Resource
	private DbBuildHistoryLogService dbBuildHistoryLogService;
	@Resource
	private SshService sshService;

	@Resource
	private BuildInfoService buildInfoService;
	@Resource
	private RepositoryService repositoryService;


	/**
	 * @return
	 * @author Hotstrip
	 * get build group list
	 * 获取构建分组列表
	 */
	@RequestMapping(value = "/build/group/list", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String groupList() {
		List<String> groupList = buildInfoService.listGroup();
		return JsonMessage.getString(200, "success", groupList);
	}

	/**
	 * load build list with params
	 *
	 * @param group
	 * @return
	 */
	@RequestMapping(value = "/build/list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.LIST)
	public String getBuildList(String group,
							   @ValidatorConfig(value = {
									   @ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "limit error")
							   }, defaultVal = "10") int limit,
							   @ValidatorConfig(value = {
									   @ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "page error")
							   }, defaultVal = "1") int page) {
		// if group is empty string, dont set into entity property
		Entity where = Entity.create()
				.setIgnoreNull(Const.GROUP_COLUMN_STR, StrUtil.isEmpty(group) ? null : group);
		Page pageReq = new Page(page, limit);
		// load list with page
		List<BuildInfoModel> list = buildInfoService.listPage(where, pageReq);
		return JsonMessage.getString(200, "success", list);
	}

	/**
	 * edit build info
	 *
	 * @param id
	 * @param name
	 * @param repositoryId
	 * @param resultDirFile
	 * @param script
	 * @param releaseMethod
	 * @param branchName
	 * @param group
	 * @param extraData
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/build/edit", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@OptLog(UserOperateLogV1.OptType.EditBuild)
	@Feature(method = MethodFeature.EDIT)
	public String updateMonitor(String id,
								@ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "构建名称不能为空")) String name,
								@ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "仓库信息不能为空")) String repositoryId,
								@ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "构建产物目录不能为空,长度1-200", range = "1:200")) String resultDirFile,
								@ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "构建命令不能为空")) String script,
								@ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "发布方法不正确") int releaseMethod,
								String branchName, String group,
								String extraData) throws Exception {
		// 根据 repositoryId 查询仓库信息
		RepositoryModel repositoryModel = repositoryService.getByKey(repositoryId);
		if (null == repositoryModel) {
			return JsonMessage.getString(405, "无效的仓库信息");
		}
		// 如果是 GIT 需要检测分支是否存在
		if (BuildModel.RepoType.Git.getCode() == repositoryModel.getRepoType()) {
			if (StrUtil.isEmpty(branchName)) {
				return JsonMessage.getString(405, "请选择分支");
			}
			List<String> list = GitUtil.getBranchList(repositoryModel.getGitUrl(), repositoryModel.getUserName(), repositoryModel.getPassword());
			if (!list.contains(branchName)) {
				return JsonMessage.getString(405, "没有找到对应分支：" + branchName);
			}
		}
		// 判断删除
		if (CommandUtil.checkContainsDel(script)) {
			return JsonMessage.getString(405, "不能包含删除命令");
		}

		// 查询构建信息
		BuildInfoModel buildInfoModel = buildInfoService.getByKey(id);
		if (null == buildInfoModel) {
			buildInfoModel = new BuildInfoModel();
			buildInfoModel.setId(IdUtil.fastSimpleUUID());
		}
		buildInfoModel.setGroup(group);
		buildInfoModel.setName(name);
		// 如果是 SVN
		if (BuildModel.RepoType.Svn.getCode() == repositoryModel.getRepoType()) {
			branchName = "trunk";
		}
		buildInfoModel.setBranchName(branchName);
		buildInfoModel.setResultDirFile(resultDirFile);
		buildInfoModel.setScript(script);
		// 设置修改人
		buildInfoModel.setModifyUser(UserModel.getOptUserName(getUser()));

		// 发布方式
		BuildModel.ReleaseMethod releaseMethod1 = BaseEnum.getEnum(BuildModel.ReleaseMethod.class, releaseMethod);
		if (releaseMethod1 == null) {
			return JsonMessage.getString(405, "发布方法不正确");
		}

		// 把 extraData 信息转换成 JSON 字符串
		JSONObject jsonObject = JSON.parseObject(extraData);

		// 验证发布方式 和 extraData 信息
		if (releaseMethod1 == BuildModel.ReleaseMethod.Project) {
			String formatProject = formatProject(jsonObject);
			if (formatProject != null) {
				return formatProject;
			}
		} else if (releaseMethod1 == BuildModel.ReleaseMethod.Ssh) {
			String formatSsh = formatSsh(jsonObject);
			if (formatSsh != null) {
				return formatSsh;
			}
		}

		// 设置属性
		buildInfoModel.setReleaseMethod(releaseMethod1.getCode());
		buildInfoModel.setExtraData(extraData);

		// 新增构建信息
		if (StrUtil.isEmpty(id)) {
			buildInfoService.add(buildInfoModel);
			return JsonMessage.getString(200, "添加成功");
		}

		buildInfoService.update(buildInfoModel);
		return JsonMessage.getString(200, "修改成功");
	}

	/**
	 * 验证构建信息
	 * 当发布方式为【SSH】的时候
	 * @param jsonObject
	 * @return
	 */
	private String formatSsh(JSONObject jsonObject) {
		// 发布方式
		String releaseMethodDataId = jsonObject.getString("releaseMethodDataId_3");
		if (StrUtil.isEmpty(releaseMethodDataId)) {
			return JsonMessage.getString(405, "请选择分发SSH项");
		}
		String releasePath = jsonObject.getString("releasePath");
		if (StrUtil.isEmpty(releasePath)) {
			return JsonMessage.getString(405, "请输入发布到ssh中的目录");
		}
		releasePath = FileUtil.normalize(releasePath);
		SshModel sshServiceItem = sshService.getItem(releaseMethodDataId);
		if (sshServiceItem == null) {
			return JsonMessage.getString(405, "没有对应的ssh项");
		}
		if (releasePath.startsWith(StrUtil.SLASH)) {
			// 以根路径开始
			List<String> fileDirs = sshServiceItem.getFileDirs();
			if (fileDirs == null || fileDirs.isEmpty()) {
				return JsonMessage.getString(405, "此ssh未授权操作此目录");
			}
			boolean find = false;
			for (String fileDir : fileDirs) {
				if (FileUtil.isSub(new File(fileDir), new File(releasePath))) {
					find = true;
				}
			}
			if (!find) {
				return JsonMessage.getString(405, "此ssh未授权操作此目录");
			}
		}
		// 发布命令
		String releaseCommand = jsonObject.getString("releaseCommand");
		if (StrUtil.isNotEmpty(releaseCommand)) {
			int length = releaseCommand.length();
			Assert.state(length <= 4000, "发布命令长度限制在4000字符");
			//return JsonMessage.getString(405, "请输入发布命令");
			String[] commands = StrUtil.splitToArray(releaseCommand, StrUtil.LF);

			for (String commandItem : commands) {
				if (!SshModel.checkInputItem(sshServiceItem, commandItem)) {
					return JsonMessage.getString(405, "发布命令中包含禁止执行的命令");
				}
			}
		}
		return null;
	}

	/**
	 * 验证构建信息
	 * 当发布方式为【项目】的时候
	 * @param jsonObject
	 * @return
	 */
	private String formatProject(JSONObject jsonObject) {
		String afterOpt = jsonObject.getString("afterOpt");
		AfterOpt afterOpt1 = BaseEnum.getEnum(AfterOpt.class, Convert.toInt(afterOpt, 0));
		if (afterOpt1 == null) {
			return JsonMessage.getString(400, "请选择打包后的操作");
		}
		return null;
	}

	/**
	 * 获取分支信息
	 * @param url
	 * @param userName
	 * @param userPwd
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/build/branch-list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String branchList(
			@ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "仓库地址不正确")) String url,
			@ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "登录账号")) String userName,
			@ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "登录密码")) String userPwd) throws Exception {
		List<String> list = GitUtil.getBranchList(url, userName, userPwd);
		return JsonMessage.getString(200, "ok", list);
	}


	/**
	 * 删除构建信息
	 * @param id
	 * @return
	 */
	@RequestMapping(value = "/build/delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@OptLog(UserOperateLogV1.OptType.DelBuild)
	@Feature(method = MethodFeature.DEL)
	public String delete(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据id") String id) {
		// 查询构建信息
		BuildInfoModel buildInfoModel = buildInfoService.getByKey(id);
		Objects.requireNonNull(buildInfoModel, "没有对应数据");
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
	 * @param id
	 * @return
	 */
	@RequestMapping(value = "/build/clean-source", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@OptLog(UserOperateLogV1.OptType.BuildCleanSource)
	@Feature(method = MethodFeature.EXECUTE)
	public String cleanSource(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据id") String id) {
		// 查询构建信息
		BuildInfoModel buildInfoModel = buildInfoService.getByKey(id);
		Objects.requireNonNull(buildInfoModel, "没有对应数据");

		File source = BuildUtil.getSourceById(buildInfoModel.getId());
		boolean del = FileUtil.del(source);
		if (!del) {
			del = FileUtil.del(source.toPath());
		}
		return JsonMessage.getString(200, "清理成功", del);
	}

}
