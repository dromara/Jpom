package io.jpom.controller.build;

import cn.hutool.db.Entity;
import cn.hutool.db.Page;
import cn.hutool.db.PageResult;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorConfig;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import com.alibaba.fastjson.JSONObject;
import io.jpom.model.data.RepositoryModel;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.dblog.RepositoryService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.stream.Collectors;

/**
 * @author Hotstrip
 * Repository controller
 */
@RestController
@Feature(cls = ClassFeature.BUILD)
public class RepositoryController {

	@Resource
	private RepositoryService repositoryService;

	/**
	 * load repository list
	 * @param limit
	 * @param page
	 * @param repoType 仓库类型 0: GIT 1: SVN
	 * @return
	 */
	@PostMapping(value = "/build/repository/list")
	@Feature(method = MethodFeature.LOG)
	public Object loadRepositoryList(@ValidatorConfig(value = {@ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "limit error")}, defaultVal = "10") int limit,
									 @ValidatorConfig(value = {@ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "page error")}, defaultVal = "1") int page,
									 Integer repoType) {
		Page pageObj = new Page(page, limit);
		Entity entity = Entity.create();
		//  add param
		if (null != repoType) {
			entity.set("repo_type", repoType);
		}
		PageResult<RepositoryModel> pageResult = repositoryService.listPage(entity, pageObj);
		JSONObject jsonObject = JsonMessage.toJson(200, "获取成功", pageResult);
		jsonObject.put("total", pageResult.getTotal());
		return jsonObject;
	}
}
