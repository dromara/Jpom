package io.jpom.controller.system;

import cn.jiangzeyin.common.JsonMessage;
import com.alibaba.fastjson.JSON;
import io.jpom.common.BaseServerController;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.system.WorkspaceService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * @author bwcx_jzy
 * @since 2021/12/3
 */
@RestController
@Feature(cls = ClassFeature.SYSTEM)
@RequestMapping(value = "/system/workspace/")
public class WorkspaceController extends BaseServerController {

	private final WorkspaceService workspaceService;

	public WorkspaceController(WorkspaceService workspaceService) {
		this.workspaceService = workspaceService;
	}


}
