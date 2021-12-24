package io.jpom.service.node.script;

import cn.hutool.http.HttpStatus;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.ScriptExecuteLogModel;
import io.jpom.service.h2db.BaseNodeService;
import io.jpom.service.node.NodeService;
import io.jpom.service.system.WorkspaceService;
import org.springframework.stereotype.Service;

/**
 * 脚本默认执行记录
 *
 * @author bwcx_jzy
 * @since 2021/12/12
 */
@Service
public class ScriptExecuteLogServer extends BaseNodeService<ScriptExecuteLogModel> {

	protected ScriptExecuteLogServer(NodeService nodeService,
									 WorkspaceService workspaceService) {
		super(nodeService, workspaceService, "脚本模版日志");
	}

	@Override
	protected String[] clearTimeColumns() {
		return new String[]{"createTimeMillis"};
	}

	@Override
	public JSONObject getItem(NodeModel nodeModel, String id) {
		return null;
	}

	@Override
	public JSONArray getLitDataArray(NodeModel nodeModel) {
		return null;
	}

	@Override
	public void syncAllNode() {
		//
	}

	@Override
	protected void executeClearImpl(int h2DbLogStorageCount) {
		super.autoLoopClear("createTimeMillis", h2DbLogStorageCount,
				null,
				executeLogModel -> {
					try {
						NodeModel nodeModel = nodeService.getByKey(executeLogModel.getNodeId());
						JsonMessage<Object> jsonMessage = NodeForward.requestBySys(nodeModel, NodeUrl.SCRIPT_DEL_LOG,
								"id", executeLogModel.getScriptId(), "executeId", executeLogModel.getId());
						if (jsonMessage.getCode() != HttpStatus.HTTP_OK) {
							DefaultSystemLog.getLog().info(jsonMessage.toString());
						}
					} catch (Exception e) {
						DefaultSystemLog.getLog().error("自动清除数据错误", e);
					}
				});
	}
}
