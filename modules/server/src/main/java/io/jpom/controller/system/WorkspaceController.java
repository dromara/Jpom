package io.jpom.controller.system;

import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import io.jpom.common.BaseServerController;
import io.jpom.model.PageResultDto;
import io.jpom.model.data.WorkspaceModel;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.system.WorkspaceService;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

	/**
	 * 编辑工作空间
	 *
	 * @param name        工作空间名称
	 * @param description 描述
	 * @return json
	 */
	@PostMapping(value = "/edit")
	@Feature(method = MethodFeature.EDIT)
	public String create(String id, @ValidatorItem String name, @ValidatorItem String description) {
		this.checkInfo(id, name);
		//
		WorkspaceModel workspaceModel = new WorkspaceModel();
		workspaceModel.setName(name);
		workspaceModel.setDescription(description);
		if (StrUtil.isEmpty(id)) {
			// 创建
			workspaceService.insert(workspaceModel);
		} else {
			workspaceModel.setId(id);
			workspaceService.update(workspaceModel);
		}
		return JsonMessage.getString(200, "操作成功");
	}

	private void checkInfo(String id, String name) {
		Entity entity = Entity.create();
		entity.set("name", name);
		if (StrUtil.isNotEmpty(id)) {
			entity.set("id", StrUtil.format(" <> {}", id));
		}
		boolean exists = workspaceService.exists(entity);
		Assert.state(!exists, "对应的工作空间名称已经存在啦");
	}

	/**
	 * 工作空间分页列表
	 *
	 * @return json
	 */
	@PostMapping(value = "/list")
	@Feature(method = MethodFeature.LIST)
	public String list() {
		PageResultDto<WorkspaceModel> listPage = workspaceService.listPage(getRequest());
		return JsonMessage.getString(200, "", listPage);
	}

	/**
	 * 查询工作空间列表
	 *
	 * @return json
	 */
	@GetMapping(value = "/list_all")
	@Feature(method = MethodFeature.LIST)
	public String listAll() {
		List<WorkspaceModel> list = workspaceService.list();
		return JsonMessage.getString(200, "", list);
	}
}
