package io.jpom.controller.node;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.http.HttpStatus;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.controller.multipart.MultipartFileBuilder;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.common.JpomManifest;
import io.jpom.common.Type;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.common.interceptor.OptLog;
import io.jpom.model.AgentFileModel;
import io.jpom.model.data.NodeModel;
import io.jpom.model.log.UserOperateLogV1;
import io.jpom.permission.SystemPermission;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.node.AgentFileService;
import io.jpom.service.node.ssh.SshService;
import io.jpom.system.ServerConfigBean;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 节点管理
 *
 * @author jiangzeyin
 * @date 2019/4/16
 */
@Controller
@RequestMapping(value = "/node")
@Feature(cls = ClassFeature.NODE)
public class NodeIndexController extends BaseServerController {

	@Resource
	private SshService sshService;
	@Resource
	private AgentFileService agentFileService;

	private List<NodeModel> listByGroup(String group) {
		List<NodeModel> nodeModels = nodeService.list();
		//
		if (nodeModels != null && StrUtil.isNotEmpty(group)) {
			// 筛选
			List<NodeModel> filterList = nodeModels.stream().filter(nodeModel -> StrUtil.equals(group, nodeModel.getGroup())).collect(Collectors.toList());
			if (CollUtil.isNotEmpty(filterList)) {
				// 如果传入的分组找到了节点，就返回  否则返回全部
				nodeModels = filterList;
			}
		}
		return nodeModels;
	}


	@RequestMapping(value = "list_data.json", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.LIST)
	@ResponseBody
	public String listJson(String group) {
		List<NodeModel> nodeModels = this.listByGroup(group);
		return JsonMessage.getString(200, "", nodeModels);
	}


	@RequestMapping(value = "list_group.json", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.LIST)
	@ResponseBody
	public String listAllGroup() {
		HashSet<String> allGroup = nodeService.getAllGroup();
		return JsonMessage.getString(200, "", allGroup);
	}

	@RequestMapping(value = "node_status", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@Feature(method = MethodFeature.LIST)
	public String nodeStatus() {
		long timeMillis = System.currentTimeMillis();
		JSONObject jsonObject = NodeForward.requestData(getNode(), NodeUrl.Status, getRequest(), JSONObject.class);
		if (jsonObject == null) {
			return JsonMessage.getString(500, "获取信息失败");
		}
		JSONArray jsonArray = new JSONArray();
		jsonObject.put("timeOut", System.currentTimeMillis() - timeMillis);
		jsonArray.add(jsonObject);
		return JsonMessage.getString(200, "", jsonArray);
	}

	/**
	 * @param status 节点状态获取
	 * @return
	 * @author Hotstrip
	 * load node project list
	 * 加载节点项目列表
	 */
	@RequestMapping(value = "node_project_list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public String nodeProjectList(@RequestParam(value = "status", defaultValue = "false") Boolean status) {
		List<NodeModel> nodeModels = null;
		if (status) {
			nodeModels = nodeService.listAndProjectAndStatus();
		} else {
			nodeModels = nodeService.listAndProject();
		}

		return JsonMessage.getString(200, "success", nodeModels);
	}

	@RequestMapping(value = "upload_agent", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@OptLog(UserOperateLogV1.OptType.UpdateSys)
	@SystemPermission
	public String uploadAgent() throws IOException {
		String saveDir = ServerConfigBean.getInstance().getAgentPath().getAbsolutePath();
		MultipartFileBuilder multipartFileBuilder = createMultipart();
		multipartFileBuilder
				.setFileExt("jar", "zip")
				.addFieldName("file")
				.setUseOriginalFilename(true)
				.setSavePath(saveDir);
		String path = multipartFileBuilder.save();
		// 解析压缩包
		File file = JpomManifest.zipFileFind(path, Type.Agent, saveDir);
		path = FileUtil.getAbsolutePath(file);
		// 基础检查
		JsonMessage<Tuple> error = JpomManifest.checkJpomJar(path, Type.Agent.getApplicationClass(), false);
		if (error.getCode() != HttpStatus.HTTP_OK) {
			FileUtil.del(path);
			return error.toString();
		}
		// 保存Agent文件
		String id = Type.Agent.name().toLowerCase();

		AgentFileModel agentFileModel = agentFileService.getItem(id);
		if (agentFileModel == null) {
			agentFileModel = new AgentFileModel();
			agentFileModel.setId(id);
			agentFileService.addItem(agentFileModel);
		}
		agentFileModel.setName(file.getName());
		agentFileModel.setSize(file.length());
		agentFileModel.setSavePath(path);
		//
		Tuple data = error.getData();
		agentFileModel.setVersion(data.get(0));
		agentFileModel.setTimeStamp(data.get(1));
		agentFileService.updateItem(agentFileModel);
		// 删除历史包  @author jzy 2021-08-03
		List<File> files = FileUtil.loopFiles(saveDir, pathname -> !FileUtil.equals(pathname, file));
		for (File file1 : files) {
			FileUtil.del(file1);
		}
		return JsonMessage.getString(200, "上传成功");
	}
}
