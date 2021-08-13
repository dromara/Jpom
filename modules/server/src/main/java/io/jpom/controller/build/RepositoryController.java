package io.jpom.controller.build;

import cn.hutool.core.lang.Validator;
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
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author Hotstrip
 * Repository controller
 */
@RestController
@Feature(cls = ClassFeature.BUILD_REPOSITORY)
public class RepositoryController {

	@Resource
	private RepositoryService repositoryService;

	/**
	 * load repository list
	 *
	 * @param limit    每页条数
	 * @param page     页码
	 * @param repoType 仓库类型 0: GIT 1: SVN
	 * @return json
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
	 *
	 * @param repositoryModelReq
	 * @return
	 */
	@PostMapping(value = "/build/repository/edit")
	@Feature(method = MethodFeature.EDIT)
	public Object editRepository(RepositoryModel repositoryModelReq) {
		this.checkInfo(repositoryModelReq);
		if (null == repositoryModelReq.getId()) {
			// insert data
			repositoryService.insert(repositoryModelReq);
		} else {
			// update data
			repositoryService.updateById(repositoryModelReq);
		}
		return JsonMessage.toJson(200, "操作成功");
	}

	/**
	 * 检查信息
	 *
	 * @param repositoryModelReq 仓库信息
	 */
	private void checkInfo(RepositoryModel repositoryModelReq) {
		Assert.hasText(repositoryModelReq.getName(), "请填写仓库名称");
		Integer repoType = repositoryModelReq.getRepoType();
		Assert.state(repoType != null && (repoType == 1 || repoType == 0), "请选择仓库类型");
		Assert.hasText(repositoryModelReq.getGitUrl(), "请填写仓库地址");
		//
		Integer protocol = repositoryModelReq.getProtocol();
		Assert.state(protocol != null && (protocol == 1 || protocol == 0), "请选择拉取代码的协议");
		// 判断仓库是否重复
		Entity entity = Entity.create();
		if (repositoryModelReq.getId() != null) {
			Validator.validateGeneral(repositoryModelReq.getId(), "错误的ID");
			entity.set("id", "<> " + repositoryModelReq.getId());
		}
		entity.set("gitUrl", repositoryModelReq.getGitUrl());
		Assert.state(!repositoryService.exists(entity), "已经存在对应的仓库信息啦");
	}

	/**
	 * delete
	 *
	 * @param id
	 * @return
	 */
	@PostMapping(value = "/build/repository/delete")
	@Feature(method = MethodFeature.DEL)
	public Object delRepository(String id) {
		repositoryService.delByKey(id);
		return JsonMessage.getString(200, "删除成功");
	}
}
