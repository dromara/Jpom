package io.jpom.service.node.script;

import io.jpom.model.data.ScriptExecuteLogModel;
import io.jpom.service.h2db.BaseWorkspaceService;
import org.springframework.stereotype.Service;

/**
 * 脚本默认执行记录
 *
 * @author bwcx_jzy
 * @since 2021/12/12
 */
@Service
public class ScriptExecuteLogServer extends BaseWorkspaceService<ScriptExecuteLogModel> {

	@Override
	protected String[] clearTimeColumns() {
		return new String[]{"createTimeMillis"};
	}
}
