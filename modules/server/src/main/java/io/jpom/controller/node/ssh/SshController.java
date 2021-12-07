package io.jpom.controller.node.ssh;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.StrSplitter;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.extra.ssh.JschUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import com.alibaba.fastjson.JSONObject;
import com.jcraft.jsch.Session;
import io.jpom.common.BaseServerController;
import io.jpom.common.Type;
import io.jpom.model.PageResultDto;
import io.jpom.model.data.AgentWhitelist;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.SshModel;
import io.jpom.model.enums.BuildReleaseMethod;
import io.jpom.model.log.SshTerminalExecuteLog;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.dblog.BuildInfoService;
import io.jpom.service.dblog.SshTerminalExecuteLogService;
import io.jpom.service.node.ssh.SshService;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.Charset;
import java.util.List;

/**
 * @author bwcx_jzy
 * @date 2019/8/9
 */
@RestController
@RequestMapping(value = "node/ssh")
@Feature(cls = ClassFeature.SSH)
public class SshController extends BaseServerController {

	private final SshService sshService;
	private final SshTerminalExecuteLogService sshTerminalExecuteLogService;
	private final BuildInfoService buildInfoService;

	public SshController(SshService sshService,
						 SshTerminalExecuteLogService sshTerminalExecuteLogService,
						 BuildInfoService buildInfoService) {
		this.sshService = sshService;
		this.sshTerminalExecuteLogService = sshTerminalExecuteLogService;
		this.buildInfoService = buildInfoService;
	}


	@PostMapping(value = "list_data.json", produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.LIST)
	public JsonMessage<PageResultDto<SshModel>> listData() {
		PageResultDto<SshModel> pageResultDto = sshService.listPage(getRequest());
		return new JsonMessage<>(200, "", pageResultDto);
	}

	@GetMapping(value = "list_data_all.json", produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.LIST)
	public JsonMessage<List<SshModel>> listDataAll() {
		List<SshModel> list = sshService.listByWorkspace(getRequest());
		return new JsonMessage<>(200, "", list);
	}

	/**
	 * 编辑
	 *
	 * @param name              名称
	 * @param host              端口
	 * @param user              用户名
	 * @param password          密码
	 * @param connectType       连接方式
	 * @param privateKey        私钥
	 * @param port              端口
	 * @param charset           编码格式
	 * @param fileDirs          文件夹
	 * @param id                ID
	 * @param notAllowedCommand 禁止输入的命令
	 * @return json
	 */
	@PostMapping(value = "save.json", produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.EDIT)
	public String save(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "ssh名称不能为空") String name,
					   @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "host不能为空") String host,
					   @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "user不能为空") String user,
					   String password,
					   SshModel.ConnectType connectType,
					   String privateKey,
					   @ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "port错误") int port,
					   String charset, String fileDirs,
					   String id, String notAllowedCommand) {
		SshModel sshModel;
		boolean add = StrUtil.isEmpty(getParameter("id"));
		if (add) {
			// 优先判断参数 如果是 password 在修改时可以不填写
			if (connectType == SshModel.ConnectType.PASS) {
				Assert.hasText(password, "请填写登录密码");
			} else if (connectType == SshModel.ConnectType.PUBKEY) {
				Assert.hasText(privateKey, "请填写证书内容");
			}
			sshModel = new SshModel();
		} else {
			sshModel = sshService.getByKey(id);
			Assert.notNull(sshModel, "不存在对应ssh");
		}
		// 目录
		if (StrUtil.isEmpty(fileDirs)) {
			sshModel.fileDirs(null);
		} else {
			List<String> list = StrSplitter.splitTrim(fileDirs, StrUtil.LF, true);
			sshModel.fileDirs(list);
		}
		sshModel.setHost(host);
		// 如果密码传递不为空就设置值 因为上面已经判断了只有修改的情况下 password 才可能为空
		if (StrUtil.isNotEmpty(password)) {
			sshModel.setPassword(password);
		}
		if (StrUtil.isNotEmpty(privateKey)) {
			sshModel.setPrivateKey(privateKey);
		}

		sshModel.setPort(port);
		sshModel.setUser(user);
		sshModel.setName(name);
		sshModel.setNotAllowedCommand(notAllowedCommand);
		sshModel.setConnectType(connectType.name());
		// 获取允许编辑的后缀
		String allowEditSuffix = getParameter("allowEditSuffix");
		List<String> allowEditSuffixList = AgentWhitelist.parseToList(allowEditSuffix, "允许编辑的文件后缀不能为空");
		sshModel.allowEditSuffix(allowEditSuffixList);
		try {
			Charset.forName(charset);
			sshModel.setCharset(charset);
		} catch (Exception e) {
			return JsonMessage.getString(405, "请填写正确的编码格式");
		}
		// 判断重复
		HttpServletRequest request = getRequest();
		String workspaceId = sshService.getCheckUserWorkspace(request);
		Entity entity = Entity.create();
		entity.set("host", sshModel.getHost());
		entity.set("port", sshModel.getPort());
		entity.set("workspaceId", workspaceId);
		if (StrUtil.isNotEmpty(id)) {
			entity.set("id", StrUtil.format(" <> {}", id));
		}
		boolean exists = sshService.exists(entity);
		Assert.state(!exists, "对应的SSH已经存在啦");
		try {
			Session session = add ? SshService.getSessionByModel(sshModel) : SshService.getSession(id);
			JschUtil.close(session);
		} catch (Exception e) {
			return JsonMessage.getString(505, "ssh连接失败：" + e.getMessage());
		}
		if (add) {
			sshService.insert(sshModel);
		} else {
			sshService.update(sshModel);
		}
		return JsonMessage.getString(200, "操作成功");
	}


	/**
	 * 检查 ssh 是否安装插件端
	 *
	 * @param ids ids
	 * @return json
	 */
	@GetMapping(value = "check_agent.json", produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.LIST)
	public String checkAgent(String ids) {
		List<SshModel> sshModels = sshService.listById(StrUtil.split(ids, StrUtil.DOT), getRequest());
		Assert.notEmpty(sshModels, "没有任何节点信息");

		JSONObject result = new JSONObject();
		for (SshModel sshModel : sshModels) {
			List<NodeModel> nodeBySshId = nodeService.getNodeBySshId(sshModel.getId());
			JSONObject data = new JSONObject();
			NodeModel nodeModel = CollUtil.getFirst(nodeBySshId);
			if (nodeModel == null) {
				try {
					SshModel model = sshService.getByKey(sshModel.getId(), false);
					Integer pid = sshService.checkSshRunPid(model, Type.Agent.getTag());
					data.put("pid", ObjectUtil.defaultIfNull(pid, 0));
					data.put("ok", true);
				} catch (Exception e) {
					DefaultSystemLog.getLog().error("检查运行状态异常:{}", e.getMessage());
					data.put("error", e.getMessage());
				}
			} else {
				data.put("nodeId", nodeModel.getId());
				data.put("nodeName", nodeModel.getName());
			}
			result.put(sshModel.getId(), data);
		}
		return JsonMessage.getString(200, "", result);
	}

	@PostMapping(value = "del.json", produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.DEL)
	public String del(@ValidatorItem(value = ValidatorRule.NOT_BLANK) String id) {
		boolean checkSsh = buildInfoService.checkReleaseMethod(id, BuildReleaseMethod.Ssh);
		Assert.state(!checkSsh, "当前ssh存在构建项，不能删除");
		sshService.delByKey(id, getRequest());
		return JsonMessage.getString(200, "操作成功");
	}

	/**
	 * 执行记录
	 *
	 * @return json
	 */
	@PostMapping(value = "log_list_data.json", produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(cls = ClassFeature.SSH_TERMINAL_LOG, method = MethodFeature.LIST)
	public String logListData() {
		PageResultDto<SshTerminalExecuteLog> pageResult = sshTerminalExecuteLogService.listPage(getRequest());
		return JsonMessage.getString(200, "获取成功", pageResult);
	}

}
