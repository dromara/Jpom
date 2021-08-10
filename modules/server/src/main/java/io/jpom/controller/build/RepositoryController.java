package io.jpom.controller.build;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.Page;
import cn.hutool.db.PageResult;
import cn.hutool.db.sql.Direction;
import cn.hutool.db.sql.Order;
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
		pageObj.addOrder(new Order("modifyTime", Direction.DESC));
		Entity entity = Entity.create();
		//  add param
		if (null != repoType) {
			entity.set("repoType", repoType);
		}
		PageResult<RepositoryModel> pageResult = repositoryService.listPage(entity, pageObj);
		JSONObject jsonObject = JsonMessage.toJson(200, "获取成功", pageResult);
		jsonObject.put("total", pageResult.getTotal());
		return jsonObject;
	}

	/**
	 * edit
	 * @param repositoryModelReq
	 * @return
	 */
	@PostMapping(value = "/build/repository/edit")
	public Object editRepository(RepositoryModel repositoryModelReq) {
		if (null == repositoryModelReq.getId()) {
			// insert data
			if (null == repositoryModelReq.getModifyTime()) {
				repositoryModelReq.setModifyTime(LocalDateTimeUtil.format(LocalDateTimeUtil.now(), "YYYY-MM-dd HH:mm:ss"));
			}
			repositoryModelReq.setId(IdUtil.fastSimpleUUID());
			repositoryService.insert(repositoryModelReq);
		} else {
			// update data
			repositoryModelReq.setModifyTime(LocalDateTimeUtil.format(LocalDateTimeUtil.now(), "YYYY-MM-dd HH:mm:ss"));
			repositoryService.updateById(repositoryModelReq);
		}
		return JsonMessage.toJson(200, "edit success");
	}

	/**
	 * delete
	 * @param id
	 * @return
	 */
	@PostMapping(value = "/build/repository/delete")
	public Object delRepository(String id) {
		repositoryService.deleteById(id);
		return JsonMessage.getString(200, "delete repository success");
	}
}
