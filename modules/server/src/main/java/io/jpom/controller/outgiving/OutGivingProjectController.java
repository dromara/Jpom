package io.jpom.controller.outgiving;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import cn.jiangzeyin.controller.multipart.MultipartFileBuilder;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.common.interceptor.OptLog;
import io.jpom.model.AfterOpt;
import io.jpom.model.BaseEnum;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.OutGivingModel;
import io.jpom.model.data.OutGivingNodeProject;
import io.jpom.model.data.ServerWhitelist;
import io.jpom.model.log.UserOperateLogV1;
import io.jpom.outgiving.OutGivingRun;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.node.OutGivingServer;
import io.jpom.service.node.manage.ProjectInfoService;
import io.jpom.service.system.ServerWhitelistServer;
import io.jpom.system.ConfigBean;
import io.jpom.system.ServerConfigBean;
import io.jpom.util.StringUtil;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 分发文件管理
 *
 * @author jiangzeyin
 * @date 2019/4/21
 */
@RestController
@RequestMapping(value = "/outgiving")
@Feature(cls = ClassFeature.OUTGIVING)
public class OutGivingProjectController extends BaseServerController {

	@Resource
	private OutGivingServer outGivingServer;
	@Resource
	private ProjectInfoService projectInfoService;
	@Resource
	private ServerWhitelistServer serverWhitelistServer;

	@RequestMapping(value = "getProjectStatus", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public String getProjectStatus() {
		return NodeForward.request(getNode(), getRequest(), NodeUrl.Manage_GetProjectStatus).toString();
	}


	@RequestMapping(value = "getItemData.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public String getItemData(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "id error") String id) throws IOException {
		OutGivingModel outGivingServerItem = outGivingServer.getItem(id);
		Objects.requireNonNull(outGivingServerItem, "没有数据");
		List<OutGivingNodeProject> outGivingNodeProjectList = outGivingServerItem.getOutGivingNodeProjectList();
		JSONArray jsonArray = new JSONArray();
		outGivingNodeProjectList.forEach(outGivingNodeProject -> {
			NodeModel nodeModel = nodeService.getItem(outGivingNodeProject.getNodeId());
			JSONObject jsonObject = new JSONObject();
			JSONObject projectInfo = null;
			try {
				projectInfo = projectInfoService.getItem(nodeModel, outGivingNodeProject.getProjectId());
			} catch (Exception e) {
				jsonObject.put("errorMsg", "error " + e.getMessage());
			}

			jsonObject.put("nodeId", outGivingNodeProject.getNodeId());
			jsonObject.put("projectId", outGivingNodeProject.getProjectId());
			jsonObject.put("nodeName", nodeModel.getName());
			if (projectInfo != null) {
				jsonObject.put("projectName", projectInfo.getString("name"));
			}

			// set projectStatus property
			//NodeModel node = nodeService.getItem(outGivingNodeProject.getNodeId());
			// Project Status: data.pid > 0 means running
			JSONObject projectStatus = JsonMessage.toJson(200, "success");
			if (nodeModel.isOpenStatus()) {
				projectStatus = NodeForward.requestBySys(nodeModel, NodeUrl.Manage_GetProjectStatus, "id", outGivingNodeProject.getProjectId()).toJson();
			}
			if (projectStatus.getJSONObject("data") != null && projectStatus.getJSONObject("data").getInteger("pId") != null) {
				jsonObject.put("projectStatus", projectStatus.getJSONObject("data").getIntValue("pId") > 0);
			} else {
				jsonObject.put("projectStatus", false);
			}

			jsonObject.put("outGivingStatus", outGivingNodeProject.getStatusMsg());
			jsonObject.put("outGivingResult", outGivingNodeProject.getResult());
			jsonObject.put("lastTime", outGivingNodeProject.getLastOutGivingTime());
			jsonArray.add(jsonObject);
		});
		return JsonMessage.getString(200, "", jsonArray);
	}

	/**
	 * 节点分发文件
	 *
	 * @param id       分发id
	 * @param afterOpt 之后的操作
	 * @return json
	 * @throws IOException IO
	 */
	@RequestMapping(value = "upload", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@OptLog(UserOperateLogV1.OptType.UploadOutGiving)
	@Feature(method = MethodFeature.UPLOAD)
	public String upload(String id, String afterOpt, String clearOld) throws IOException {
		OutGivingModel outGivingModel = this.check(id);
		AfterOpt afterOpt1 = BaseEnum.getEnum(AfterOpt.class, Convert.toInt(afterOpt, 0));
		Assert.notNull(afterOpt1, "请选择分发后的操作");
		MultipartFileBuilder multipartFileBuilder = createMultipart();
		multipartFileBuilder
				.setFileExt(StringUtil.PACKAGE_EXT)
				.addFieldName("file")
				.setSavePath(ServerConfigBean.getInstance().getUserTempPath().getAbsolutePath());
		String path = multipartFileBuilder.save();
		//
		File src = FileUtil.file(path);
		File dest = null;
		for (String i : StringUtil.PACKAGE_EXT) {
			if (FileUtil.pathEndsWith(src, i)) {
				dest = FileUtil.file(ConfigBean.getInstance().getDataPath(), ServerConfigBean.OUTGIVING_FILE, id + "." + i);
				break;
			}
		}
		Assert.notNull(dest, "不支持的文件类型");
		FileUtil.move(src, dest, true);
		//
		outGivingModel = outGivingServer.getItem(id);
		outGivingModel.setClearOld(Convert.toBool(clearOld, false));
		outGivingModel.setAfterOpt(afterOpt1.getCode());

		outGivingServer.updateItem(outGivingModel);
		// 开启
		OutGivingRun.startRun(outGivingModel.getId(), dest, getUser(), true);
		return JsonMessage.getString(200, "分发成功");
	}

	private OutGivingModel check(String id) {
		OutGivingModel outGivingModel = outGivingServer.getItem(id);
		Assert.notNull(outGivingModel, "上传失败,没有找到对应的分发项目");
		// 检查状态
		List<OutGivingNodeProject> outGivingNodeProjectList = outGivingModel.getOutGivingNodeProjectList();
		Objects.requireNonNull(outGivingNodeProjectList);
		for (OutGivingNodeProject outGivingNodeProject : outGivingNodeProjectList) {
			Assert.state(outGivingNodeProject.getStatus() != OutGivingNodeProject.Status.Ing.getCode(), "当前还在分发中,请等待分发结束");
		}
		return outGivingModel;
	}

	/**
	 * 远程下载节点分发文件
	 *
	 * @param id       分发id
	 * @param afterOpt 之后的操作
	 * @return json
	 */
	@RequestMapping(value = "remote_download", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@OptLog(UserOperateLogV1.OptType.UploadOutGiving)
	@Feature(method = MethodFeature.REMOTE_DOWNLOAD)
	public String remoteDownload(String id, String afterOpt, String clearOld, String url, String unzip) {
		OutGivingModel outGivingModel = this.check(id);
		AfterOpt afterOpt1 = BaseEnum.getEnum(AfterOpt.class, Convert.toInt(afterOpt, 0));
		Assert.notNull(afterOpt1, "请选择分发后的操作");
		// 验证远程 地址
		ServerWhitelist whitelist = serverWhitelistServer.getWhitelist();
		Set<String> allowRemoteDownloadHost = whitelist.getAllowRemoteDownloadHost();
		Assert.state(CollUtil.isNotEmpty(allowRemoteDownloadHost), "还没有配置运行的远程地址");
		List<String> collect = allowRemoteDownloadHost.stream().filter(s -> StrUtil.startWith(url, s)).collect(Collectors.toList());
		Assert.state(CollUtil.isNotEmpty(collect), "不允许下载当前地址的文件");
		try {
			outGivingModel = outGivingServer.getItem(id);
			outGivingModel.setClearOld(Convert.toBool(clearOld, false));
			outGivingModel.setAfterOpt(afterOpt1.getCode());
			outGivingServer.updateItem(outGivingModel);
			//下载
			File file = FileUtil.file(ServerConfigBean.getInstance().getUserTempPath(), ServerConfigBean.OUTGIVING_FILE, id);
			FileUtil.mkdir(file);
			File downloadFile = HttpUtil.downloadFileFromUrl(url, file);
			// 开启
			OutGivingRun.startRun(outGivingModel.getId(), downloadFile, getUser(), BooleanUtil.toBoolean(unzip));
			return JsonMessage.getString(200, "分发成功");
		} catch (Exception e) {
			DefaultSystemLog.getLog().error("下载远程文件异常", e);
			return JsonMessage.getString(500, "下载远程文件失败:" + e.getMessage());
		}
	}
}
