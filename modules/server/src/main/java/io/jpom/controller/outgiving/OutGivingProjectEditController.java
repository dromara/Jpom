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
package io.jpom.controller.outgiving;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpStatus;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.common.Const;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.AfterOpt;
import io.jpom.model.BaseEnum;
import io.jpom.model.RunMode;
import io.jpom.model.data.AgentWhitelist;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.ServerWhitelist;
import io.jpom.model.data.UserModel;
import io.jpom.model.enums.BuildReleaseMethod;
import io.jpom.model.outgiving.OutGivingModel;
import io.jpom.model.outgiving.OutGivingNodeProject;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.service.dblog.BuildInfoService;
import io.jpom.service.node.ProjectInfoCacheService;
import io.jpom.service.outgiving.OutGivingServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * 节点分发编辑项目
 *
 * @author jiangzeyin
 * @since 2019/4/22
 */
@RestController
@RequestMapping(value = "/outgiving")
@Feature(cls = ClassFeature.OUTGIVING)
@Slf4j
public class OutGivingProjectEditController extends BaseServerController {

	private final OutGivingWhitelistService outGivingWhitelistService;
	private final OutGivingServer outGivingServer;
	private final ProjectInfoCacheService projectInfoCacheService;
	private final BuildInfoService buildService;

	public OutGivingProjectEditController(OutGivingWhitelistService outGivingWhitelistService,
										  OutGivingServer outGivingServer,
										  ProjectInfoCacheService projectInfoCacheService,
										  BuildInfoService buildService) {
		this.outGivingWhitelistService = outGivingWhitelistService;
		this.outGivingServer = outGivingServer;
		this.projectInfoCacheService = projectInfoCacheService;
		this.buildService = buildService;
	}

	/**
	 * 保存节点分发项目
	 *
	 * @param id   id
	 * @param type 类型
	 * @return json
	 */
	@RequestMapping(value = "save_project", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.EDIT)
	public String save(@ValidatorItem String id, String type) {
		if ("add".equalsIgnoreCase(type)) {
			//boolean general = StringUtil.isGeneral(id, 2, 20);
			//Assert.state(general, "分发id 不能为空并且长度在2-20（英文字母 、数字和下划线）");
			String checkId = StrUtil.replace(id, StrUtil.DASHED, StrUtil.UNDERLINE);
			Validator.validateGeneral(checkId, 2, Const.ID_MAX_LEN, "分发id 不能为空并且长度在2-20（英文字母 、数字和下划线）");
			return addOutGiving(id);
		} else {
			return updateGiving(id);
		}
	}

	/**
	 * 删除分发项目
	 *
	 * @param id 项目id
	 * @return json
	 */
	@RequestMapping(value = "delete_project", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.DEL)
	public String delete(String id) {
		HttpServletRequest request = getRequest();
		OutGivingModel outGivingModel = outGivingServer.getByKey(id, request);
		Assert.notNull(outGivingModel, "没有对应的分发项目");

		// 判断构建
		boolean releaseMethod = buildService.checkReleaseMethod(id, request, BuildReleaseMethod.Outgiving);
		Assert.state(!releaseMethod, "当前分发存在构建项，不能删除");
		//
		Assert.state(outGivingModel.outGivingProject(), "该项目不是节点分发项目,不能在此次删除");

		UserModel userModel = getUser();
		List<OutGivingNodeProject> deleteNodeProject = outGivingModel.outGivingNodeProjectList();
		if (deleteNodeProject != null) {
			// 删除实际的项目
			for (OutGivingNodeProject outGivingNodeProject1 : deleteNodeProject) {
				NodeModel nodeModel = nodeService.getByKey(outGivingNodeProject1.getNodeId());
				JsonMessage<String> jsonMessage = this.deleteNodeProject(nodeModel, userModel, outGivingNodeProject1.getProjectId());
				if (jsonMessage.getCode() != HttpStatus.HTTP_OK) {
					return JsonMessage.getString(406, nodeModel.getName() + "节点失败：" + jsonMessage.getMsg());
				}
			}
		}

		outGivingServer.delByKey(id, request);
		return JsonMessage.getString(200, "删除成功");
	}

	private String addOutGiving(String id) {
		OutGivingModel outGivingModel = outGivingServer.getByKey(id);
		Assert.isNull(outGivingModel, "分发id已经存在啦");

		outGivingModel = new OutGivingModel();
		outGivingModel.setOutGivingProject(true);
		outGivingModel.setId(id);
		//
		List<Tuple> tuples = doData(outGivingModel, false);

		outGivingServer.insert(outGivingModel);
		String error = saveNodeData(outGivingModel, tuples, false);
		if (error != null) {
			return error;
		}
		return JsonMessage.getString(200, "添加成功");
	}


	private String updateGiving(String id) {
		OutGivingModel outGivingModel = outGivingServer.getByKey(id, getRequest());
		Assert.notNull(outGivingModel, "没有找到对应的分发id");
		List<Tuple> tuples = doData(outGivingModel, true);

		outGivingServer.update(outGivingModel);
		String error = saveNodeData(outGivingModel, tuples, true);
		if (error != null) {
			return error;
		}
		return JsonMessage.getString(200, "修改成功");
	}

	/**
	 * 保存节点项目数据
	 *
	 * @param outGivingModel 节点分发项目
	 * @param edit           是否为编辑模式
	 * @return 错误信息
	 */
	private String saveNodeData(OutGivingModel outGivingModel, List<Tuple> tuples, boolean edit) {

//		if () {
//			if (!edit) {
//				outGivingServer.delByKey(outGivingModel.getId());
//			}
//			return JsonMessage.getString(405, "数据异常,请重新操作");
//		}
		UserModel userModel = getUser();
		List<Tuple> success = new ArrayList<>();
		boolean fail = false;
		try {
			for (Tuple tuple : tuples) {
				NodeModel nodeModel = tuple.get(0);
				JSONObject data = tuple.get(1);
				//
				JsonMessage<String> jsonMessage = this.sendData(nodeModel, userModel, data, true);
				if (jsonMessage.getCode() != HttpStatus.HTTP_OK) {
					if (!edit) {
						fail = true;
						outGivingServer.delByKey(outGivingModel.getId());
					}
					return JsonMessage.getString(406, nodeModel.getName() + "节点失败：" + jsonMessage.getMsg());
				}
				success.add(tuple);
				// 同步项目信息
				projectInfoCacheService.syncNode(nodeModel, outGivingModel.getId());
			}
		} catch (Exception e) {
			log.error("保存分发项目失败", e);
			if (!edit) {
				fail = true;
				outGivingServer.delByKey(outGivingModel.getId());
			}
			return JsonMessage.getString(500, "保存节点数据失败:" + e.getMessage());
		} finally {
			if (fail) {
				try {
					for (Tuple entry : success) {
						deleteNodeProject(entry.get(0), userModel, outGivingModel.getId());
					}
				} catch (Exception e) {
					log.error("还原项目失败", e);
				}
			}
		}
		return null;
	}

	/**
	 * 删除项目
	 *
	 * @param nodeModel 节点
	 * @param userModel 用户
	 * @param project   判断id
	 * @return json
	 */
	private JsonMessage<String> deleteNodeProject(NodeModel nodeModel, UserModel userModel, String project) {
		JSONObject data = new JSONObject();
		data.put("id", project);
		JsonMessage<String> request = NodeForward.request(nodeModel, NodeUrl.Manage_DeleteProject, userModel, data);
		if (request.getCode() == HttpStatus.HTTP_OK) {
			// 同步项目信息
			projectInfoCacheService.syncNode(nodeModel, project);
		}
		return request;
//        // 发起预检查数据
//        String url = nodeModel.getRealUrl(NodeUrl.Manage_DeleteProject);
//        HttpRequest request = HttpUtil.createPost(url);
//        // 授权信息
//        NodeForward.addUser(request, nodeModel, userModel);

//        request.form(data);
//        //
//        String body = request.execute()
//                .body();
//        return NodeForward.toJsonMessage(body);
	}

	/**
	 * 创建项目管理的默认数据
	 *
	 * @param outGivingModel 分发实体
	 * @param edit           是否为编辑模式
	 * @return String为有异常
	 */
	private JSONObject getDefData(OutGivingModel outGivingModel, boolean edit) {
		JSONObject defData = new JSONObject();
		defData.put("id", outGivingModel.getId());
		defData.put("name", outGivingModel.getName());
		//
		// 运行模式
		String runMode = getParameter("runMode");
		RunMode runMode1 = RunMode.ClassPath;
		try {
			runMode1 = RunMode.valueOf(runMode);
		} catch (Exception ignored) {
		}
		defData.put("runMode", runMode1.name());
		if (runMode1 == RunMode.ClassPath || runMode1 == RunMode.JavaExtDirsCp) {
			String mainClass = getParameter("mainClass");
			defData.put("mainClass", mainClass);
		}
		if (runMode1 == RunMode.JavaExtDirsCp) {
			defData.put("javaExtDirsCp", getParameter("javaExtDirsCp"));
		}
		if (runMode1 == RunMode.Dsl) {
			defData.put("dslContent", getParameter("dslContent"));
		}
		String whitelistDirectory = getParameter("whitelistDirectory");
		ServerWhitelist configDeNewInstance = outGivingWhitelistService.getServerWhitelistData(getRequest());
		List<String> whitelistServerOutGiving = configDeNewInstance.getOutGiving();
		Assert.state(AgentWhitelist.checkPath(whitelistServerOutGiving, whitelistDirectory), "请选择正确的项目路径,或者还没有配置白名单");

		defData.put("whitelistDirectory", whitelistDirectory);
		String logPath = getParameter("logPath");
		if (StrUtil.isNotEmpty(logPath)) {
			Assert.state(AgentWhitelist.checkPath(whitelistServerOutGiving, logPath), "请选择正确的日志路径,或者还没有配置白名单");
			defData.put("logPath", logPath);
		}
		String lib = getParameter("lib");
		defData.put("lib", lib);
		if (edit) {
			// 编辑模式
			defData.put("edit", "on");
		}
		defData.put("previewData", true);
		return defData;
	}

	/**
	 * 处理页面数据
	 *
	 * @param outGivingModel 分发实体
	 * @param edit           是否为编辑模式
	 */
	private List<Tuple> doData(OutGivingModel outGivingModel, boolean edit) {
		outGivingModel.setName(getParameter("name"));
		Assert.hasText(outGivingModel.getName(), "分发名称不能为空");
		//
		int intervalTime = getParameterInt("intervalTime", 10);
		outGivingModel.setIntervalTime(intervalTime);
		outGivingModel.setClearOld(Convert.toBool(getParameter("clearOld"), false));
		//
		String nodeIdsStr = getParameter("nodeIds");
		List<String> nodeIds = StrUtil.splitTrim(nodeIdsStr, StrUtil.COMMA);
		//List<NodeModel> nodeModelList = nodeService.listByWorkspace(getRequest());
		Assert.notEmpty(nodeIds, "没有任何节点信息");

		//
		String afterOpt = getParameter("afterOpt");
		AfterOpt afterOpt1 = BaseEnum.getEnum(AfterOpt.class, Convert.toInt(afterOpt, 0));
		Assert.notNull(afterOpt1, "请选择分发后的操作");
		outGivingModel.setAfterOpt(afterOpt1.getCode());
		JSONObject defData = getDefData(outGivingModel, edit);

		UserModel userModel = getUser();
		//
		List<OutGivingModel> outGivingModels = outGivingServer.list();
		List<OutGivingNodeProject> outGivingNodeProjects = new ArrayList<>();
		OutGivingNodeProject outGivingNodeProject;
		//
		//Iterator<NodeModel> iterator = nodeModelList.iterator();


		List<Tuple> tuples = new ArrayList<>();

		for (String nodeId : nodeIds) {
			NodeModel nodeModel = nodeService.getByKey(nodeId);
			Assert.notNull(nodeModel, "对应的节点不存在");
			//String add = getParameter("add_" + nodeModel.getId());
//			if (!nodeModel.getId().equals(add)) {
//				iterator.remove();
//				continue;
//			}
			// 判断项目是否已经被使用过啦
			if (outGivingModels != null) {
				for (OutGivingModel outGivingModel1 : outGivingModels) {
					if (outGivingModel1.getId().equalsIgnoreCase(outGivingModel.getId())) {
						continue;
					}
					Assert.state(!outGivingModel1.checkContains(nodeModel.getId(), outGivingModel.getId()), "已经存在相同的分发项目:" + outGivingModel.getId());

				}
			}
			outGivingNodeProject = outGivingModel.getNodeProject(nodeModel.getId(), outGivingModel.getId());
			if (outGivingNodeProject == null) {
				outGivingNodeProject = new OutGivingNodeProject();
			}
			outGivingNodeProject.setNodeId(nodeModel.getId());
			outGivingNodeProject.setProjectId(outGivingModel.getId());
			outGivingNodeProjects.add(outGivingNodeProject);
			// 检查数据
			JSONObject allData = defData.clone();
			String token = getParameter(StrUtil.format("{}_token", nodeModel.getId()));
			allData.put("token", token);
			String jvm = getParameter(StrUtil.format("{}_jvm", nodeModel.getId()));
			allData.put("jvm", jvm);
			String args = getParameter(StrUtil.format("{}_args", nodeModel.getId()));
			allData.put("args", args);
			String autoStart = getParameter(StrUtil.format("{}_autoStart", nodeModel.getId()));
			allData.put("autoStart", Convert.toBool(autoStart, false));
			// 项目副本
			String javaCopyIds = getParameter(StrUtil.format("{}_javaCopyIds", nodeModel.getId()));
			allData.put("javaCopyIds", javaCopyIds);
			if (StrUtil.isNotEmpty(javaCopyIds)) {
				String[] split = StrUtil.splitToArray(javaCopyIds, StrUtil.COMMA);
				for (String copyId : split) {
					String copyJvm = getParameter(StrUtil.format("{}_jvm_{}", nodeModel.getId(), copyId));
					String copyArgs = getParameter(StrUtil.format("{}_args_{}", nodeModel.getId(), copyId));
                    String nameArgs = getParameter(StrUtil.format("{}_name_{}", nodeModel.getId(), copyId));
					allData.put("jvm_" + copyId, copyJvm);
					allData.put("args_" + copyId, copyArgs);
                    allData.put("name_" + copyId, nameArgs);
				}
			}
			JsonMessage<String> jsonMessage = this.sendData(nodeModel, userModel, allData, false);
			Assert.state(jsonMessage.getCode() == HttpStatus.HTTP_OK, nodeModel.getName() + "节点失败：" + jsonMessage.getMsg());
			tuples.add(new Tuple(nodeModel, allData));
		}
		// 删除已经删除的项目
		deleteProject(outGivingModel, outGivingNodeProjects, userModel);

		outGivingModel.outGivingNodeProjectList(outGivingNodeProjects);

		return tuples;
	}

	/**
	 * 删除已经删除过的项目
	 *
	 * @param outGivingModel        分发项目
	 * @param outGivingNodeProjects 新的节点项目
	 * @param userModel             用户
	 */
	private void deleteProject(OutGivingModel outGivingModel, List<OutGivingNodeProject> outGivingNodeProjects, UserModel userModel) {
		Assert.state(CollUtil.size(outGivingNodeProjects) >= 2, "至少选择两个节点及以上");
		// 删除
		List<OutGivingNodeProject> deleteNodeProject = outGivingModel.getDelete(outGivingNodeProjects);
		if (deleteNodeProject != null) {
			JsonMessage<String> jsonMessage;
			// 删除实际的项目
			for (OutGivingNodeProject outGivingNodeProject1 : deleteNodeProject) {
				NodeModel nodeModel = nodeService.getByKey(outGivingNodeProject1.getNodeId());
				//outGivingNodeProject1.getNodeData(true);
				jsonMessage = this.deleteNodeProject(nodeModel, userModel, outGivingNodeProject1.getProjectId());
				Assert.state(jsonMessage.getCode() == HttpStatus.HTTP_OK, nodeModel.getName() + "节点失败：" + jsonMessage.getMsg());
			}
		}
	}

	private JsonMessage<String> sendData(NodeModel nodeModel, UserModel userModel, JSONObject data, boolean save) {
		if (save) {
			data.remove("previewData");
		}
		data.put("outGivingProject", true);
		// 发起预检查数据
		return NodeForward.request(nodeModel, NodeUrl.Manage_SaveProject, userModel, data);
	}
}
