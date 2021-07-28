package io.jpom.controller.build;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorConfig;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import io.jpom.build.BuildUtil;
import io.jpom.common.BaseServerController;
import io.jpom.common.interceptor.OptLog;
import io.jpom.model.AfterOpt;
import io.jpom.model.BaseEnum;
import io.jpom.model.data.BuildModel;
import io.jpom.model.data.SshModel;
import io.jpom.model.data.UserModel;
import io.jpom.model.log.UserOperateLogV1;
import io.jpom.model.vo.BuildModelVo;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.build.BuildService;
import io.jpom.service.dblog.DbBuildHistoryLogService;
import io.jpom.service.node.OutGivingServer;
import io.jpom.service.node.ssh.SshService;
import io.jpom.util.CommandUtil;
import io.jpom.util.GitUtil;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 构建列表
 *
 * @author bwcx_jzy
 * @date 2019/7/16
 */
@Controller
@RequestMapping(value = "/build")
@Feature(cls = ClassFeature.BUILD)
public class BuildListController extends BaseServerController {

	@Resource
	private BuildService buildService;
	@Resource
	private OutGivingServer outGivingServer;
	@Resource
	private DbBuildHistoryLogService dbBuildHistoryLogService;
	@Resource
	private SshService sshService;

//    @RequestMapping(value = "list.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
//    @Feature(method = MethodFeature.LIST)
//    public String list() {
//        //通知方式
//        JSONArray jsonArray = BaseEnum.toJSONArray(BuildModel.Status.class);
//        setAttribute("statusArray", jsonArray);
//        Set<String> set = buildService.listGroup();
//        setAttribute("groupArray", set);
//        return "build/list";
//    }

	/**
	 * @return
	 * @author Hotstrip
	 * get build group list
	 * 获取构建分组列表
	 */
	@RequestMapping(value = "group-list", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public String groupList() {
		Set<String> set = buildService.listGroup();
		return JsonMessage.getString(200, "success", set);
	}

	@RequestMapping(value = "list_data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@ResponseBody
	@Feature(method = MethodFeature.LIST)
	public String getMonitorList(String group) {
		List<BuildModelVo> list = buildService.list(BuildModelVo.class);
		if (StrUtil.isNotEmpty(group) && CollUtil.isNotEmpty(list)) {
			List<BuildModelVo> array = new ArrayList<>();
			for (BuildModelVo buildModelVo : list) {
				if (group.equals(buildModelVo.getGroup())) {
					array.add(buildModelVo);
				}
			}
			list = array;
		}
		return JsonMessage.getString(200, "", list);
	}

	@RequestMapping(value = "updateBuild", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@ResponseBody
	@OptLog(UserOperateLogV1.OptType.EditBuild)
	@Feature(method = MethodFeature.EDIT)
	public String updateMonitor(String id,
								@ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "构建名称不能为空")) String name,
								@ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "仓库地址不正确")) String gitUrl,
								@ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "登录账号")) String userName,
								@ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "登录密码")) String password,
								@ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "构建产物目录不能为空")) String resultDirFile,
								@ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "构建命令不能为空")) String script,
								@ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "发布方法不正确") int releaseMethod,
								String branchName, String group, int repoType) throws Exception {
		BaseEnum anEnum = BaseEnum.getEnum(BuildModel.RepoType.class, repoType);
		if (anEnum == null) {
			return JsonMessage.getString(405, "仓库类型选择错误");
		}
		if (BuildModel.RepoType.Git.getCode() == repoType) {
			if (StrUtil.isEmpty(branchName)) {
				return JsonMessage.getString(405, "请选择分支");
			}
			List<String> list = GitUtil.getBranchList(gitUrl, userName, password, null);
			if (!list.contains(branchName)) {
				return JsonMessage.getString(405, "没有找到对应分支：" + branchName);
			}
		}
		// 判断删除
		if (CommandUtil.checkContainsDel(script)) {
			return JsonMessage.getString(405, "不能包含删除命令");
		}
		if (StrUtil.isEmpty("group")) {
			return JsonMessage.getString(405, "请选择分组");
		}
		BuildModel buildModel = buildService.getItem(id);
		if (buildModel == null) {
			buildModel = new BuildModel();
			buildModel.setId(IdUtil.fastSimpleUUID());
		}
		buildModel.setGroup(group);
		buildModel.setName(name);
		buildModel.setRepoType(repoType);
		buildModel.setGitUrl(gitUrl);
		if (BuildModel.RepoType.Svn.getCode() == repoType) {
			branchName = "trunk";
		}
		buildModel.setBranchName(branchName);
		buildModel.setPassword(password);
		buildModel.setUserName(userName);
		buildModel.setResultDirFile(resultDirFile);
		buildModel.setScript(script);
		//
		buildModel.setModifyUser(UserModel.getOptUserName(getUser()));
		//
		BuildModel.ReleaseMethod releaseMethod1 = BaseEnum.getEnum(BuildModel.ReleaseMethod.class, releaseMethod);
		if (releaseMethod1 == null) {
			return JsonMessage.getString(405, "发布方法不正确");
		}
		buildModel.setReleaseMethod(releaseMethod1.getCode());
		if (releaseMethod1 == BuildModel.ReleaseMethod.Outgiving) {
			String releaseMethodDataId = getParameter("releaseMethodDataId_1");
			if (StrUtil.isEmpty(releaseMethodDataId)) {
				return JsonMessage.getString(405, "请选择分发项目");
			}
			buildModel.setReleaseMethodDataId(releaseMethodDataId);
		} else if (releaseMethod1 == BuildModel.ReleaseMethod.Project) {
			String formatProject = formatProject(buildModel);
			if (formatProject != null) {
				return formatProject;
			}
		} else if (releaseMethod1 == BuildModel.ReleaseMethod.Ssh) {
			String formatSsh = formatSsh(buildModel);
			if (formatSsh != null) {
				return formatSsh;
			}
		} else {
			buildModel.setReleaseMethodDataId(null);
		}
		if (StrUtil.isEmpty(id)) {
			buildService.addItem(buildModel);
			return JsonMessage.getString(200, "添加成功");
		}
		buildService.updateItem(buildModel);
		return JsonMessage.getString(200, "修改成功");
	}

	private String formatSsh(BuildModel buildModel) {
		//
		String releaseMethodDataId = getParameter("releaseMethodDataId_3");
		if (StrUtil.isEmpty(releaseMethodDataId)) {
			return JsonMessage.getString(405, "请选择分发SSH项");
		}
		String releasePath = getParameter("releasePath");
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
		//
		String releaseCommand = getParameter("releaseCommand");

		//return JsonMessage.getString(405, "请输入发布命令");
		String[] commands = StrUtil.splitToArray(releaseCommand, StrUtil.LF);

		for (String commandItem : commands) {
			if (!SshModel.checkInputItem(sshServiceItem, commandItem)) {
				return JsonMessage.getString(405, "发布命令中包含禁止执行的命令");
			}
		}

		buildModel.setReleasePath(releasePath);
		buildModel.setReleaseCommand(releaseCommand);
		buildModel.setReleaseMethodDataId(releaseMethodDataId);
		String clearOld = getParameter("clearOld");
		buildModel.setClearOld(Convert.toBool(clearOld, false));
		return null;
	}

	private String formatProject(BuildModel buildModel) {
		String releaseMethodDataId2Node = getParameter("releaseMethodDataId_2_node");
		String releaseMethodDataId2Project = getParameter("releaseMethodDataId_2_project");
		if (StrUtil.isEmpty(releaseMethodDataId2Node) || StrUtil.isEmpty(releaseMethodDataId2Project)) {
			return JsonMessage.getString(405, "请选择节点和项目");
		}
		buildModel.setReleaseMethodDataId(String.format("%s:%s", releaseMethodDataId2Node, releaseMethodDataId2Project));
		//
		String afterOpt = getParameter("afterOpt");
		AfterOpt afterOpt1 = BaseEnum.getEnum(AfterOpt.class, Convert.toInt(afterOpt, 0));
		if (afterOpt1 == null) {
			return JsonMessage.getString(400, "请选择打包后的操作");
		}
		String clearOld = getParameter("clearOld");
		buildModel.setAfterOpt(afterOpt1.getCode());
		buildModel.setClearOld(Convert.toBool(clearOld, false));
		return null;
	}

//    @RequestMapping(value = "edit.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
//    @Feature(method = MethodFeature.EDIT)
//    public String edit(String id) {
//        BuildModel buildModel = null;
//        if (StrUtil.isNotEmpty(id)) {
//            buildModel = buildService.getItem(id);
//        }
//        setAttribute("model", buildModel);
//        //
//        JSONArray releaseMethods = BaseEnum.toJSONArray(BuildModel.ReleaseMethod.class);
//        // 获取ssh 相关信息
//        List<SshModel> list = sshService.list();
//        if (list == null || list.isEmpty()) {
//            releaseMethods = releaseMethods.stream().filter(o -> {
//                JSONObject jsonObject = (JSONObject) o;
//                return jsonObject.getIntValue("code") != BuildModel.ReleaseMethod.Ssh.getCode();
//            }).collect(Collectors.toCollection(JSONArray::new));
//
//        } else {
//            //
//            setAttribute("sshArray", list);
//        }
//        setAttribute("releaseMethods", releaseMethods);
//        Set<String> set = buildService.listGroup();
//        if (set.isEmpty()) {
//            set.add("默认");
//        }
//        setAttribute("groupArray", set);
//        //
//        List<OutGivingModel> outGivingModels = outGivingServer.list();
//        setAttribute("outGivingModels", outGivingModels);
//
//        //
//        List<NodeModel> nodeModels = nodeService.listAndProject();
//        setAttribute("nodeModels", nodeModels);
//        //
//        JSONArray jsonArray = BaseEnum.toJSONArray(AfterOpt.class);
//        setAttribute("afterOpt", jsonArray);
//        //
//        JSONArray outAfterOpt = BaseEnum.toJSONArray(AfterOpt.class);
//        setAttribute("outAfterOpt", outAfterOpt);
//        //
//        JSONArray repoTypes = BaseEnum.toJSONArray(BuildModel.RepoType.class);
//        setAttribute("repoTypes", repoTypes);
//        return "build/edit";
//    }

	@RequestMapping(value = "branchList.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@ResponseBody
	public String branchList(
			@ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "仓库地址不正确")) String url,
			@ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "登录账号")) String userName,
			@ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "登录密码")) String userPwd) throws GitAPIException, IOException {
		List<String> list = GitUtil.getBranchList(url, userName, userPwd, null);
		return JsonMessage.getString(200, "ok", list);
	}


	@RequestMapping(value = "delete.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@ResponseBody
	@OptLog(UserOperateLogV1.OptType.DelBuild)
	@Feature(method = MethodFeature.DEL)
	public String delete(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据id") String id) {
		BuildModel buildModel = buildService.getItem(id);
		Objects.requireNonNull(buildModel, "没有对应数据");
		dbBuildHistoryLogService.delByBuildId(buildModel.getId());
		//
		File file = BuildUtil.getBuildDataFile(buildModel.getId());
		if (!FileUtil.del(file)) {
			FileUtil.del(file.toPath());
			return JsonMessage.getString(500, "清理历史构建产物失败,已经重新尝试");
		}
		buildService.deleteItem(buildModel.getId());
		return JsonMessage.getString(200, "清理历史构建产物成功");
	}


	@RequestMapping(value = "cleanSource.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@ResponseBody
	@OptLog(UserOperateLogV1.OptType.BuildCleanSource)
	@Feature(method = MethodFeature.EXECUTE)
	public String cleanSource(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据id") String id) {
		BuildModel buildModel = buildService.getItem(id);
		Objects.requireNonNull(buildModel, "没有对应数据");
		File source = BuildUtil.getSource(buildModel);
		boolean del = FileUtil.del(source);
		if (!del) {
			del = FileUtil.del(source.toPath());
		}
		return JsonMessage.getString(200, "清理成功", del);
	}

}
