package io.jpom.controller.manage;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.PatternPool;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import io.jpom.JpomApplication;
import io.jpom.common.BaseAgentController;
import io.jpom.common.commander.AbstractProjectCommander;
import io.jpom.model.RunMode;
import io.jpom.model.data.JdkInfoModel;
import io.jpom.model.data.ProjectInfoModel;
import io.jpom.service.WhitelistDirectoryService;
import io.jpom.service.manage.JdkInfoService;
import io.jpom.system.ConfigBean;
import io.jpom.util.StringUtil;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 编辑项目
 *
 * @author jiangzeyin
 * @date 2019/4/16
 */
@RestController
@RequestMapping(value = "/manage/")
public class ManageEditProjectController extends BaseAgentController {
	@Resource
	private WhitelistDirectoryService whitelistDirectoryService;
	@Resource
	private JdkInfoService jdkInfoService;

	/**
	 * 基础检查
	 *
	 * @param projectInfo        项目实体
	 * @param whitelistDirectory 白名单
	 * @param previewData        预检查数据
	 * @return null 检查正常
	 */
	private String checkParameter(ProjectInfoModel projectInfo, String whitelistDirectory, boolean previewData) {
		String id = projectInfo.getId();
		if (StrUtil.isEmptyOrUndefined(id)) {
			return JsonMessage.getString(400, "项目id不能为空");
		}
		if (!StringUtil.isGeneral(id, 2, 20)) {
			return JsonMessage.getString(401, "项目id 长度范围2-20（英文字母 、数字和下划线）");
		}
		if (JpomApplication.SYSTEM_ID.equals(id)) {
			return JsonMessage.getString(401, "项目id " + JpomApplication.SYSTEM_ID + " 关键词被系统占用");
		}
		// 防止和Jpom冲突
		if (StrUtil.isNotEmpty(ConfigBean.getInstance().applicationTag) && ConfigBean.getInstance().applicationTag.equalsIgnoreCase(id)) {
			return JsonMessage.getString(401, "当前项目id已经被Jpom占用");
		}
		// 运行模式
		String runMode = getParameter("runMode");
		RunMode runMode1 = RunMode.ClassPath;
		try {
			runMode1 = RunMode.valueOf(runMode);
		} catch (Exception ignored) {
		}
		projectInfo.setRunMode(runMode1);
		// 监测
		if (runMode1 == RunMode.ClassPath || runMode1 == RunMode.JavaExtDirsCp) {
			if (StrUtil.isEmpty(projectInfo.getMainClass())) {
				return JsonMessage.getString(401, "ClassPath、JavaExtDirsCp 模式 MainClass必填");
			}
		} else if (runMode1 == RunMode.Jar || runMode1 == RunMode.JarWar) {
			projectInfo.setMainClass("");
		}
		if (runMode1 == RunMode.JavaExtDirsCp) {
			if (StrUtil.isEmpty(projectInfo.getJavaExtDirsCp())) {
				return JsonMessage.getString(401, "JavaExtDirsCp 模式 javaExtDirsCp必填");
			}
		}
		// 判断是否为分发添加
		String strOutGivingProject = getParameter("outGivingProject");
		boolean outGivingProject = Boolean.parseBoolean(strOutGivingProject);

		projectInfo.setOutGivingProject(outGivingProject);
		if (!previewData) {
			// 不是预检查数据才效验白名单
			if (!whitelistDirectoryService.checkProjectDirectory(whitelistDirectory)) {
				if (outGivingProject) {
					whitelistDirectoryService.addProjectWhiteList(whitelistDirectory);
				} else {
					return JsonMessage.getString(401, "请选择正确的项目路径,或者还没有配置白名单");
				}
			}
			String logPath = projectInfo.getLogPath();
			if (StrUtil.isNotEmpty(logPath)) {
				if (!whitelistDirectoryService.checkProjectDirectory(logPath)) {
					return JsonMessage.getString(401, "请填写的项目日志存储路径,或者还没有配置白名单");
				}
			}
		}
		//
		String lib = projectInfo.getLib();
		if (StrUtil.isEmpty(lib) || StrUtil.SLASH.equals(lib) || Validator.isChinese(lib)) {
			return JsonMessage.getString(401, "项目Jar路径不能为空,不能为顶级目录,不能包含中文");
		}
		if (!checkPathSafe(lib)) {
			return JsonMessage.getString(401, "项目Jar路径存在提升目录问题");
		}
		// java 程序副本
		if (runMode1 == RunMode.ClassPath || runMode1 == RunMode.Jar || runMode1 == RunMode.JarWar || runMode1 == RunMode.JavaExtDirsCp) {
			String javaCopyIds = getParameter("javaCopyIds");
			if (StrUtil.isEmpty(javaCopyIds)) {
				projectInfo.setJavaCopyItemList(null);
			} else {
				String[] split = StrUtil.splitToArray(javaCopyIds, StrUtil.COMMA);
				List<ProjectInfoModel.JavaCopyItem> javaCopyItemList = new ArrayList<>(split.length);
				for (String copyId : split) {
					String jvm = getParameter("jvm_" + copyId);
					String args = getParameter("args_" + copyId);
					//
					ProjectInfoModel.JavaCopyItem javaCopyItem = new ProjectInfoModel.JavaCopyItem();
					javaCopyItem.setId(copyId);
					javaCopyItem.setParendId(id);
					javaCopyItem.setModifyTime(DateUtil.now());
					javaCopyItem.setJvm(StrUtil.emptyToDefault(jvm, StrUtil.EMPTY));
					javaCopyItem.setArgs(StrUtil.emptyToDefault(args, StrUtil.EMPTY));
					javaCopyItemList.add(javaCopyItem);
				}
				projectInfo.setJavaCopyItemList(javaCopyItemList);
			}
		} else {
			projectInfo.setJavaCopyItemList(null);
		}
		return null;
	}


	@RequestMapping(value = "saveProject", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public String saveProject(ProjectInfoModel projectInfo) {
		// 预检查数据
		String strPreviewData = getParameter("previewData");
		boolean previewData = Convert.toBool(strPreviewData, false);
		String whitelistDirectory = projectInfo.getWhitelistDirectory();
		//
		String error = checkParameter(projectInfo, whitelistDirectory, previewData);
		if (error != null) {
			return error;
		}
		String id = projectInfo.getId();
		//
		String allLib = projectInfo.allLib();
		// 重复lib
		List<ProjectInfoModel> list = projectInfoService.list();
		if (list != null) {
			for (ProjectInfoModel projectInfoModel : list) {
				if (!projectInfoModel.getId().equals(id) && projectInfoModel.allLib().equals(allLib)) {
					return JsonMessage.getString(401, "当前项目Jar路径已经被【" + projectInfoModel.getName() + "】占用,请检查");
				}
			}
		}
		File checkFile = new File(allLib);
		if (checkFile.exists() && checkFile.isFile()) {
			return JsonMessage.getString(401, "项目Jar路径是一个已经存在的文件");
		}
		// 自动生成log文件
//		String log = new File(allLib).getParent();
//		log = String.format("%s/%s.log", log, id);
//		projectInfo.setLog(FileUtil.normalize(log));
		String log = projectInfo.getLog();
		if (StrUtil.isEmpty(log)) {
			return JsonMessage.getString(401, "项目log解析读取失败");
		}
		checkFile = new File(log);
		if (checkFile.exists() && checkFile.isDirectory()) {
			return JsonMessage.getString(401, "项目log是一个已经存在的文件夹");
		}
		//
		String token = projectInfo.getToken();
		if (StrUtil.isNotEmpty(token) && !ReUtil.isMatch(PatternPool.URL_HTTP, token)) {
			return JsonMessage.getString(401, "WebHooks 地址不合法");
		}
		// 判断空格
		if (id.contains(StrUtil.SPACE) || allLib.contains(StrUtil.SPACE)) {
			return JsonMessage.getString(401, "项目Id、项目Jar不能包含空格");
		}
		String jdkId = projectInfo.getJdkId();
		if (StrUtil.isNotEmpty(jdkId)) {
			JdkInfoModel item = jdkInfoService.getItem(jdkId);
			if (null == item) {
				return JsonMessage.getString(401, "jdk 信息错误");
			}
		}
		return save(projectInfo, previewData);
	}

	/**
	 * 保存项目
	 *
	 * @param projectInfo 项目
	 * @param previewData 是否是预检查
	 * @return 错误信息
	 */
	private String save(ProjectInfoModel projectInfo, boolean previewData) {
		String edit = getParameter("edit");
		ProjectInfoModel exits = projectInfoService.getItem(projectInfo.getId());
		try {
			JsonMessage<String> jsonMessage = checkPath(projectInfo);
			if (jsonMessage != null) {
				return jsonMessage.toString();
			}
			if (exits == null) {
				// 检查运行中的tag 是否被占用
				if (AbstractProjectCommander.getInstance().isRun(projectInfo.getId())) {
					return JsonMessage.getString(400, "当前项目id已经被正在运行的程序占用");
				}
				if (previewData) {
					// 预检查数据
					return JsonMessage.getString(200, "检查通过");
				} else {
					projectInfoService.addItem(projectInfo);
					return JsonMessage.getString(200, "新增成功！");
				}
			}
			if (previewData) {
				// 预检查数据
				return JsonMessage.getString(200, "检查通过");
			} else {
				exits.setLog(projectInfo.getLog());
				exits.setLogPath(projectInfo.getLogPath());
				exits.setName(projectInfo.getName());
				exits.setGroup(projectInfo.getGroup());
				exits.setMainClass(projectInfo.getMainClass());
				exits.setLib(projectInfo.getLib());
				exits.setJvm(projectInfo.getJvm());
				exits.setArgs(projectInfo.getArgs());
				exits.setOutGivingProject(projectInfo.isOutGivingProject());
				exits.setRunMode(projectInfo.getRunMode());
				exits.setWhitelistDirectory(projectInfo.getWhitelistDirectory());
				exits.setToken(projectInfo.getToken());
				exits.setJdkId(projectInfo.getJdkId());
				// 检查是否非法删除副本集
				List<ProjectInfoModel.JavaCopyItem> javaCopyItemList = exits.getJavaCopyItemList();
				List<ProjectInfoModel.JavaCopyItem> javaCopyItemList1 = projectInfo.getJavaCopyItemList();
				if (CollUtil.isNotEmpty(javaCopyItemList) && !CollUtil.containsAll(javaCopyItemList1, javaCopyItemList)) {
					// 重写了 equals
					return JsonMessage.getString(405, "修改中不能删除副本集、请到副本集中删除");
				}
				exits.setJavaCopyItemList(javaCopyItemList1);
				exits.setJavaExtDirsCp(projectInfo.getJavaExtDirsCp());
				//
				moveTo(exits, projectInfo);
				projectInfoService.updateItem(exits);
				return JsonMessage.getString(200, "修改成功");
			}
		} catch (Exception e) {
			DefaultSystemLog.getLog().error(e.getMessage(), e);
			return JsonMessage.getString(500, "保存数据异常");
		}
	}

	private void moveTo(ProjectInfoModel old, ProjectInfoModel news) {
		// 移动目录
		if (!old.allLib().equals(news.allLib())) {
			File oldLib = new File(old.allLib());
			if (oldLib.exists()) {
				File newsLib = new File(news.allLib());
				FileUtil.move(oldLib, newsLib, true);
			}
		}
		// log
		if (!old.getLog().equals(news.getLog())) {
			File oldLog = new File(old.getLog());
			if (oldLog.exists()) {
				File newsLog = new File(news.getLog());
				FileUtil.move(oldLog, newsLog, true);
			}
			// logBack
			File oldLogBack = old.getLogBack();
			if (oldLogBack.exists()) {
				FileUtil.move(oldLogBack, news.getLogBack(), true);
			}
		}

	}

	/**
	 * 路径存在包含关系
	 *
	 * @param projectInfoModel 比较的项目
	 * @return 不为null 则为错误
	 */
	private JsonMessage<String> checkPath(ProjectInfoModel projectInfoModel) {
		List<ProjectInfoModel> projectInfoModelList = projectInfoService.list();
		if (projectInfoModelList == null) {
			return null;
		}
		ProjectInfoModel projectInfoModel1 = null;
		for (ProjectInfoModel model : projectInfoModelList) {
			if (!model.getId().equals(projectInfoModel.getId())) {
				File file1 = new File(model.allLib());
				File file2 = new File(projectInfoModel.allLib());
				if (FileUtil.pathEquals(file1, file2)) {
					projectInfoModel1 = model;
					break;
				}
				// 包含关系
				if (FileUtil.isSub(file1, file2) || FileUtil.isSub(file2, file1)) {
					projectInfoModel1 = model;
					break;
				}
			}
		}
		if (projectInfoModel1 != null) {
			return new JsonMessage<>(401, "项目Jar路径和【" + projectInfoModel1.getName() + "】项目冲突:" + projectInfoModel1.allLib());
		}
		return null;
	}

	/**
	 * 删除项目
	 *
	 * @return json
	 */
	@RequestMapping(value = "deleteProject", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public String deleteProject(String copyId) {
		ProjectInfoModel projectInfoModel = tryGetProjectInfoModel();
		if (projectInfoModel == null) {
			return JsonMessage.getString(200, "项目不存在");
		}
		try {
			ProjectInfoModel.JavaCopyItem copyItem = projectInfoModel.findCopyItem(copyId);
			if (copyItem == null) {
				// 运行判断
				if (projectInfoModel.tryGetStatus()) {
					return JsonMessage.getString(401, "不能删除正在运行的项目");
				}
				projectInfoService.deleteItem(projectInfoModel.getId());
			} else {
				if (copyItem.tryGetStatus()) {
					return JsonMessage.getString(401, "不能删除正在运行的项目副本");
				}
				boolean removeCopyItem = projectInfoModel.removeCopyItem(copyId);
				if (!removeCopyItem) {
					return JsonMessage.getString(200, "删除对应副本集不存在");
				}
				projectInfoService.updateItem(projectInfoModel);
			}
			return JsonMessage.getString(200, "删除成功！");
		} catch (Exception e) {
			DefaultSystemLog.getLog().error(e.getMessage(), e);
			return JsonMessage.getString(500, "删除异常：" + e.getMessage());
		}
	}

	@RequestMapping(value = "releaseOutGiving", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public String releaseOutGiving() {
		ProjectInfoModel projectInfoModel = tryGetProjectInfoModel();
		if (projectInfoModel != null) {
			projectInfoModel.setOutGivingProject(false);
			projectInfoService.updateItem(projectInfoModel);
		}
		return JsonMessage.getString(200, "ok");
	}

	/**
	 * 检查项目lib 情况
	 *
	 * @param id     项目id
	 * @param newLib 新路径
	 * @return 状态码，400是一定不能操作的，401 是提醒
	 */
	@RequestMapping(value = "judge_lib.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public String saveProject(String id, String newLib) {
		File file = new File(newLib);
		//  填写的jar路径是一个存在的文件
		if (file.exists() && file.isFile()) {
			return JsonMessage.getString(400, "填写jar目录当前是一个已经存在的文件,请修改");
		}
		ProjectInfoModel exits = projectInfoService.getItem(id);
		if (exits == null) {
			// 创建项目 填写的jar路径是已经存在的文件夹
			if (file.exists()) {
				return JsonMessage.getString(401, "填写jar目录当前已经在,创建成功后会自动同步文件");
			}
		} else {
			// 已经存在的项目
			File oldLib = new File(exits.allLib());
			Path newPath = file.toPath();
			Path oldPath = oldLib.toPath();
			if (newPath.equals(oldPath)) {
				// 新 旧没有变更
				return JsonMessage.getString(200, "");
			}
			if (file.exists()) {
				if (oldLib.exists()) {
					// 新旧jar路径都存在，会自动覆盖新的jar路径中的文件
					return JsonMessage.getString(401, "原jar目录已经存在并且新的jar目录已经存在,保存将覆盖新文件夹并会自动同步原jar目录");
				}
				return JsonMessage.getString(401, "填写jar目录当前已经在,创建成功后会自动同步文件");
			}
		}
		if (Validator.isChinese(newLib)) {
			return JsonMessage.getString(401, "不建议使用中文目录");
		}
		return JsonMessage.getString(200, "");
	}
}
