package io.jpom.service.node;

import io.jpom.common.BaseOperService;
import io.jpom.common.Type;
import io.jpom.model.AgentFileModel;
import io.jpom.system.ServerConfigBean;
import org.springframework.stereotype.Service;

/**
 * @author lf
 */
@Service
public class AgentFileService extends BaseOperService<AgentFileModel> {

	/**
	 * 保存Agent文件
	 */
	public static final String ID = Type.Agent.name().toLowerCase();

	public AgentFileService() {
		super(ServerConfigBean.AGENT_FILE);
	}
}
