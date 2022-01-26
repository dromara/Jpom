package io.jpom.service.docker;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.SystemClock;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.cron.task.Task;
import com.alibaba.fastjson.JSONObject;
import io.jpom.cron.CronUtils;
import io.jpom.cron.IAsyncLoad;
import io.jpom.model.docker.DockerInfoModel;
import io.jpom.plugin.IPlugin;
import io.jpom.plugin.PluginFactory;
import io.jpom.service.h2db.BaseWorkspaceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @author bwcx_jzy
 * @since 2022/1/26
 */
@Service
@Slf4j
public class DockerInfoService extends BaseWorkspaceService<DockerInfoModel> implements IAsyncLoad, Task {

	private static final String CRON_ID = "docker-monitor";

	public static final String DOCKER_CHECK_PLUGIN_NAME = "docker-cli:check";

	@Override
	public void startLoad() {
		CronUtils.add(CRON_ID, "0 0/1 * * * ?", () -> DockerInfoService.this);
	}

	@Override
	public void execute() {
		List<DockerInfoModel> list = this.list();
		if (CollUtil.isEmpty(list)) {
			return;
		}
		this.checkList(list);
	}

	private void checkList(List<DockerInfoModel> monitorModels) {
		monitorModels.forEach(monitorModel -> ThreadUtil.execute(() -> DockerInfoService.this.monitorItem(monitorModel)));
	}

	/**
	 * 监控 容器
	 *
	 * @param dockerInfoModel docker
	 */
	private void monitorItem(DockerInfoModel dockerInfoModel) {
		try {
			IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_CHECK_PLUGIN_NAME);
			Map<String, Object> parameter = dockerInfoModel.toParameter();
			parameter.put("timeout", dockerInfoModel.getHeartbeatTimeout());
			//
			JSONObject version = plugin.execute("pullVersion", parameter, JSONObject.class);
			//
			DockerInfoModel update = new DockerInfoModel();
			update.setId(dockerInfoModel.getId());
			update.setStatus(1);
			update.setLastHeartbeatTime(SystemClock.now());
			update.setDockerVersion(version.toJSONString());
			update.setFailureMsg(StrUtil.EMPTY);
			super.update(update);
		} catch (Exception e) {
			log.error("监控 docker 异常", e);
			this.updateStatus(dockerInfoModel.getId(), 0, e.getMessage());
		}
	}

	/**
	 * 更新 容器状态
	 *
	 * @param id     ID
	 * @param status 状态值
	 * @param msg    错误消息
	 */
	private void updateStatus(String id, int status, String msg) {
		DockerInfoModel dockerInfoModel = new DockerInfoModel();
		dockerInfoModel.setId(id);
		dockerInfoModel.setStatus(status);
		dockerInfoModel.setFailureMsg(msg);
		super.update(dockerInfoModel);
	}
}
