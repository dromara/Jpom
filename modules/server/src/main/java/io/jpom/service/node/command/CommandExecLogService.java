package io.jpom.service.node.command;

import cn.hutool.core.io.FileUtil;
import io.jpom.model.data.CommandExecLogModel;
import io.jpom.service.h2db.BaseWorkspaceService;
import io.jpom.util.CommandUtil;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * @author bwcx_jzy
 * @since 2021/12/22
 */
@Service
public class CommandExecLogService extends BaseWorkspaceService<CommandExecLogModel> {

	@Override
	protected void fillSelectResult(CommandExecLogModel data) {
		if (data == null) {
			return;
		}
		data.setHasLog(FileUtil.exist(data.logFile()));
	}

	@Override
	protected void executeClearImpl(int h2DbLogStorageCount) {
		super.autoLoopClear("createTimeMillis", h2DbLogStorageCount, null, commandExecLogModel -> {
			File file = commandExecLogModel.logFile();
			CommandUtil.systemFastDel(file);
			File parentFile = file.getParentFile();
			boolean empty = FileUtil.isEmpty(parentFile);
			if (empty) {
				CommandUtil.systemFastDel(parentFile);
			}
		});
	}

	@Override
	protected String[] clearTimeColumns() {
		return super.clearTimeColumns();
	}
}
