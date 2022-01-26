package io.jpom.controller.docker;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.db.Entity;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.controller.multipart.MultipartFileBuilder;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.model.PageResultDto;
import io.jpom.model.docker.DockerInfoModel;
import io.jpom.plugin.*;
import io.jpom.service.docker.DockerInfoService;
import io.jpom.system.ServerConfigBean;
import io.jpom.util.CompressionFileUtil;
import io.jpom.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.List;

/**
 * @author bwcx_jzy
 * @since 2022/1/26
 */
@RestController
@Feature(cls = ClassFeature.DOCKER)
@RequestMapping(value = "/docker")
@Slf4j
public class DockerInfoController extends BaseServerController {

	private final DockerInfoService dockerInfoService;

	public DockerInfoController(DockerInfoService dockerInfoService) {
		this.dockerInfoService = dockerInfoService;
	}

	/**
	 * @return json
	 */
	@GetMapping(value = "api-versions", produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.LIST)
	public String apiVersions() throws Exception {
		IPlugin plugin = PluginFactory.getPlugin("docker-cli:check");
		List<JSONObject> data = (List<JSONObject>) plugin.execute("apiVersions");
		return JsonMessage.getString(200, "", data);
	}

	/**
	 * @return json
	 */
	@PostMapping(value = "list", produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.LIST)
	public String list() {
		// load list with page
		PageResultDto<DockerInfoModel> resultDto = dockerInfoService.listPage(getRequest());
//		IPlugin plugin = PluginFactory.getPlugin("docker-cli:check");
		resultDto.each(this::checkcertPath);
		return JsonMessage.getString(200, "", resultDto);
	}

	private boolean checkcertPath(DockerInfoModel dockerInfoModel) {
		if (dockerInfoModel == null) {
			return false;
		}
		if (dockerInfoModel.getCertExist() != null && dockerInfoModel.getCertExist()) {
			return true;
		}
		String certPath = dockerInfoModel.generateCertPath();
		IPlugin plugin = PluginFactory.getPlugin("docker-cli:check");
		try {
			boolean execute = (boolean) plugin.execute("certPath", "certPath", certPath);
			dockerInfoModel.setCertExist(execute);
			return execute;
		} catch (Exception e) {
			log.error("检查 docker 证书异常", e);
			return false;
		}
	}

	private DockerInfoModel takeOverModel(File certPathFile) throws Exception {
		IPlugin plugin = PluginFactory.getPlugin("docker-cli:check");
		String name = getParameter("name");
		Assert.hasText(name, "请填写 名称");
		String host = getParameter("host");
		String id = getParameter("id");
		String tlsVerifyStr = getParameter("tlsVerify");
		String apiVersion = getParameter("apiVersion");
		boolean tlsVerify = Convert.toBool(tlsVerifyStr, false);
		//
		boolean certExist = false;
		if (tlsVerify) {
			// 如果是创建就必须上传证书
			MultipartFileBuilder multipart = null;
			try {
				multipart = createMultipart();
			} catch (Exception e) {
				DockerInfoModel dockerInfoModel = dockerInfoService.getByKey(id);
				certExist = this.checkcertPath(dockerInfoModel);
				Assert.state(certExist, "请上传证书文件");
			}
			if (multipart != null) {
				String absolutePath = FileUtil.getAbsolutePath(certPathFile);
				multipart.setSavePath(absolutePath).addFieldName("file").setUseOriginalFilename(true);
				String localPath = multipart.setFileExt(StringUtil.PACKAGE_EXT).save();
				// 解压
				File file = new File(localPath);
				CompressionFileUtil.unCompress(file, certPathFile);
				boolean ok = (boolean) plugin.execute("certPath", "certPath", absolutePath);
				Assert.state(ok, "证书信息不正确");
				certExist = true;
			}
		}
		boolean ok = (boolean) plugin.execute("host", "host", host);
		Assert.state(ok, "请填写正确到 host");
		// 验证重复
		String workspaceId = dockerInfoService.getCheckUserWorkspace(getRequest());
		Entity entity = Entity.create();
		entity.set("host", host);
		entity.set("workspaceId", workspaceId);
		if (StrUtil.isNotEmpty(id)) {
			entity.set("id", StrUtil.format(" <> {}", id));
		}
		boolean exists = dockerInfoService.exists(entity);
		Assert.state(!exists, "对应的 docker 已经存在啦");
		//
		DockerInfoModel.DockerInfoModelBuilder builder = DockerInfoModel.builder();
		builder.apiVersion(apiVersion).host(host).name(name).tlsVerify(tlsVerify).certExist(certExist);
		//
		DockerInfoModel build = builder.build();
		build.setId(id);
		return build;
	}

	@PostMapping(value = "edit", produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.EDIT)
	public String edit(String id, String host) throws Exception {
		// 保存路径
		File tempPath = ServerConfigBean.getInstance().getUserTempPath();
		File savePath = FileUtil.file(tempPath, "docker", SecureUtil.sha1(host));
		DockerInfoModel dockerInfoModel = this.takeOverModel(savePath);
		boolean certExist = dockerInfoModel.getCertExist();
		if (StrUtil.isEmpty(id)) {
			// 创建
			if (dockerInfoModel.getTlsVerify()) {
				Assert.state(certExist, "请上传证书文件");
			}
			dockerInfoService.insert(dockerInfoModel);
		} else {
			dockerInfoService.updateById(dockerInfoModel, getRequest());
		}
		// 移动证书
		if (certExist) {
			if (FileUtil.isDirectory(savePath) && FileUtil.isNotEmpty(savePath)) {
				String generateCertPath = dockerInfoModel.generateCertPath();
				FileUtil.move(savePath, FileUtil.file(generateCertPath), true);
			}
		}
		return JsonMessage.getString(200, "操作成功");
	}
}
