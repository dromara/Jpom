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
package io.jpom.controller.ssh;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.db.Entity;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import cn.jiangzeyin.controller.multipart.MultipartFileBuilder;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.common.JpomManifest;
import io.jpom.common.Type;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.SshModel;
import io.jpom.model.system.AgentAutoUser;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.permission.SystemPermission;
import io.jpom.service.node.NodeService;
import io.jpom.service.node.ssh.SshService;
import io.jpom.system.ConfigBean;
import io.jpom.system.ExtConfigBean;
import io.jpom.system.ServerConfigBean;
import io.jpom.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileUrlResource;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * ssh 安装插件端
 *
 * @author bwcx_jzy
 * @since 2019/8/17
 */
@RestController
@RequestMapping(value = "node/ssh")
@Feature(cls = ClassFeature.SSH)
@Slf4j
public class SshInstallAgentController extends BaseServerController {

	private final SshService sshService;
	private final NodeService nodeService;

	public SshInstallAgentController(SshService sshService,
									 NodeService nodeService) {
		this.sshService = sshService;
		this.nodeService = nodeService;
	}


	private JSONObject getAgentFile() throws IOException {
		ServerConfigBean instance = ServerConfigBean.getInstance();
		File agentZipPath = instance.getAgentZipPath();
		Assert.state(FileUtil.exist(agentZipPath), "插件包文件不存在,需要重新上传");
		String tempFilePath = instance.getUserTempPath().getAbsolutePath();
		//
		File tempAgent = FileUtil.file(tempFilePath, "temp_agent");
		FileUtil.del(tempAgent);
		try {
			// 解析压缩包
			File jarFile = JpomManifest.zipFileFind(FileUtil.getAbsolutePath(agentZipPath), Type.Agent, FileUtil.getAbsolutePath(tempAgent));
			// 获取包内容
			JsonMessage<Tuple> tupleJsonMessage = JpomManifest.checkJpomJar(FileUtil.getAbsolutePath(jarFile), Type.Agent, false);

			Assert.state(tupleJsonMessage.getCode() == 200, tupleJsonMessage::getMsg);
			Tuple data = tupleJsonMessage.getData();
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("version", data.get(0));
			jsonObject.put("timeStamp", data.get(1));
			jsonObject.put("path", FileUtil.getAbsolutePath(agentZipPath));
			return jsonObject;
		} finally {
			FileUtil.del(tempAgent);
		}
	}

	@GetMapping(value = "get_agent.json", produces = MediaType.APPLICATION_JSON_VALUE)
	@SystemPermission
	public String getAgent() throws IOException {
		JSONObject agentFile = this.getAgentFile();
		return JsonMessage.getString(200, "", agentFile);
	}

	@RequestMapping(value = "upload_agent.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.EXECUTE)
	@SystemPermission
	public String uploadAgent() throws Exception {
		ServerConfigBean instance = ServerConfigBean.getInstance();
		String tempFilePath = instance.getUserTempPath().getAbsolutePath();
		MultipartFileBuilder multipartFileBuilder = createMultipart()
				.setFileExt("zip")
				.addFieldName("file")
				.setSavePath(tempFilePath);
		String filePath = multipartFileBuilder.save();
		File tempAgent = FileUtil.file(tempFilePath, "temp_agent");
		FileUtil.del(tempAgent);
		// 解析压缩包
		File jarFile = JpomManifest.zipFileFind(filePath, Type.Agent, FileUtil.getAbsolutePath(tempAgent));
		// 验证文件是否正确
		JsonMessage<Tuple> tupleJsonMessage = JpomManifest.checkJpomJar(FileUtil.getAbsolutePath(jarFile), Type.Agent, false);
		Assert.state(tupleJsonMessage.getCode() == 200, tupleJsonMessage::getMsg);
		//
		File outFle = FileUtil.file(tempFilePath, Type.Agent.name() + "_" + IdUtil.fastSimpleUUID());
		try {
			this.unZipGetTag(filePath, outFle);
			// 保存插件包
			File agentZipPath = instance.getAgentZipPath();
			FileUtil.copy(FileUtil.file(filePath), agentZipPath, true);
			return JsonMessage.getString(200, "上传成功");
		} finally {
			FileUtil.del(filePath);
			FileUtil.del(jarFile);
		}
	}

	private String unZipGetTag(String filePath, File outFle) throws IOException {
		try (ZipFile zipFile = new ZipFile(filePath)) {
			// 判断文件是否正确
			ZipEntry sh = zipFile.getEntry(Type.Agent.name() + ".sh");
			ZipEntry lib = zipFile.getEntry("lib" + StrUtil.SLASH);
			Assert.state(sh != null && lib != null && lib.isDirectory(), "不是 Jpom 插件包");
			ZipUtil.unzip(zipFile, outFle);
		}
		// 获取上传的tag
		File shFile = FileUtil.file(outFle, Type.Agent.name() + ".sh");
		List<String> lines = FileUtil.readLines(shFile, CharsetUtil.CHARSET_UTF_8);
		String tag = null;
		for (String line : lines) {
			line = line.trim();
			if (StrUtil.startWith(line, "Tag=\"") && StrUtil.endWith(line, "\"")) {
				tag = line.substring(5, line.length() - 1);
				break;
			}
		}
		Assert.hasText(tag, "管理命令中不存在tag");
		return tag;
	}

	@RequestMapping(value = "installAgentSubmit.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.EXECUTE)
	@SystemPermission
	public String installAgentSubmit(@ValidatorItem(value = ValidatorRule.NOT_BLANK) String id,
									 @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "节点数据") String nodeData,
									 @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "安装路径") String path) throws Exception {
		//
		SshModel sshModel = sshService.getByKey(id, false);
		Objects.requireNonNull(sshModel, "没有找到对应ssh");
		// 判断输入的节点信息
		NodeModel nodeModel = this.getNodeModel(nodeData, sshModel);
		//
		ServerConfigBean instance = ServerConfigBean.getInstance();
		String tempFilePath = instance.getUserTempPath().getAbsolutePath();
		JSONObject agentFile = this.getAgentFile();
		String filePath = agentFile.getString("path");
		//
		File outFle = FileUtil.file(tempFilePath, Type.Agent.name() + "_" + IdUtil.fastSimpleUUID());
		try {
			String tag = this.unZipGetTag(filePath, outFle);
			//
			this.readNodeAuthorize(outFle, nodeModel);
			// 查询远程是否运行
			Assert.state(!sshService.checkSshRun(sshModel, tag), "对应服务器中已经存在 Jpom 插件端,不需要再次安装啦");
			// 上传文件到服务器
			sshService.uploadDir(sshModel, path, outFle);
			//
			String shPtah = FileUtil.normalize(path + StrUtil.SLASH + Type.Agent.name() + ".sh");
			String chmod = getParameter("chmod");
			if (StrUtil.isEmptyOrUndefined(chmod)) {
				chmod = StrUtil.EMPTY;
			} else {
				chmod = StrUtil.format("{} {} && ", chmod, shPtah);
			}
			String command = StrUtil.format("{}bash {} start upgrade", chmod, shPtah);
			String result = sshService.exec(sshModel, command);
			log.debug("ssh install agent node {} {}", command, result);
			// 休眠 5 秒, 尝试 5 次
			int waitCount = getParameterInt("waitCount", 5);
			this.loopCheck(waitCount, nodeModel, sshModel, path, result);
			// 绑定关系
			nodeModel.setSshId(sshModel.getId());
			nodeService.insert(nodeModel);
			//
			return JsonMessage.getString(200, "操作成功:" + result);
		} finally {
			// 清理资源
			FileUtil.del(outFle);
		}
	}

	private void readNodeAuthorize(File outFle, NodeModel nodeModel) throws IOException {
		//  读取授权信息
		File configFile = FileUtil.file(outFle, ExtConfigBean.FILE_NAME);
		if (configFile.exists()) {
			YamlPropertySourceLoader yamlPropertySourceLoader = new YamlPropertySourceLoader();
			List<PropertySource<?>> extConfig = yamlPropertySourceLoader.load(ExtConfigBean.FILE_NAME, new FileUrlResource(configFile.getAbsolutePath()));
			Assert.notEmpty(extConfig, "没有加载到配置信息,或者配置信息为空");
			PropertySource<?> propertySource = extConfig.get(0);
			Object user = propertySource.getProperty(ConfigBean.AUTHORIZE_USER_KEY);
			nodeModel.setLoginName(Convert.toStr(user, ""));
			//
			Object pwd = propertySource.getProperty(ConfigBean.AUTHORIZE_AUTHORIZE_KEY);
			nodeModel.setLoginPwd(Convert.toStr(pwd, ""));
		}
	}

	private void loopCheck(int waitCount, NodeModel nodeModel, SshModel sshModel, String path, String result) {
		waitCount = Math.max(waitCount, 5);
		//int time = 3;
		while (--waitCount >= 0) {
			//log.debug("there is left {} / 3 times try to get authorize info", waitCount);
			ThreadUtil.sleep(5, TimeUnit.SECONDS);
			if (StrUtil.hasEmpty(nodeModel.getLoginName(), nodeModel.getLoginPwd())) {
				String error = this.getAuthorize(sshModel, nodeModel, path);
				// 获取授权成功就不需要继续循环了
				if (error == null) {
					waitCount = -1;
				}
				// 获取授权失败且尝试次数用完
				if (error != null && waitCount == 0) {
					throw new IllegalStateException(StrUtil.format("{} {}", error, result));
				}
			} else {
				break;
			}
		}
	}

	private String getAuthorize(SshModel sshModel, NodeModel nodeModel, String path) {
		File saveFile = null;
		try {
			String tempFilePath = ServerConfigBean.getInstance().getUserTempPath().getAbsolutePath();
			//  获取远程的授权信息
			String normalize = FileUtil.normalize(StrUtil.format("{}/{}/{}", path, ConfigBean.DATA, ConfigBean.AUTHORIZE));
			saveFile = FileUtil.file(tempFilePath, IdUtil.fastSimpleUUID() + ConfigBean.AUTHORIZE);
			sshService.download(sshModel, normalize, saveFile);
			//
			String json = FileUtil.readString(saveFile, CharsetUtil.CHARSET_UTF_8);
			AgentAutoUser autoUser = JSONObject.parseObject(json, AgentAutoUser.class);
			nodeModel.setLoginPwd(autoUser.getAgentPwd());
			nodeModel.setLoginName(autoUser.getAgentName());
		} catch (Exception e) {
			log.error("拉取授权信息失败:{}", e.getMessage());
			return "获取授权信息失败,请检查对应的插件端运行状态" + e.getMessage();
		} finally {
			FileUtil.del(saveFile);
		}
		return null;
	}

	private NodeModel getNodeModel(String data, SshModel sshModel) {
		NodeModel nodeModel = StringUtil.jsonConvert(data, NodeModel.class);
		Assert.notNull(nodeModel, "请填写节点信息");
		//JSONObject.toJavaObject(JSONObject.parseObject(data), NodeModel.class);

		Assert.hasText(nodeModel.getName(), "输入节点名称");

		Assert.hasText(nodeModel.getUrl(), "请输入节点地址");
		//nodeModel.setCycle(Cycle.one.getCode());
		//
		//nodeModel.setProtocol(StrUtil.emptyToDefault(nodeModel.getProtocol(), "http"));
		//
		Entity entity = Entity.create();
		entity.set("url", nodeModel.getUrl());
		entity.set("workspaceId", sshModel.getWorkspaceId());
		boolean exists = nodeService.exists(entity);
		Assert.state(!exists, "对应的节点已经存在啦");
		//
		nodeModel.setOpenStatus(1);

		return nodeModel;
	}

}
