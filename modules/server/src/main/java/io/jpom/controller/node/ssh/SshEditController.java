package io.jpom.controller.node.ssh;

import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import io.jpom.common.BaseServerController;
import io.jpom.model.enums.BuildReleaseMethod;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.dblog.BuildInfoService;
import io.jpom.service.node.ssh.SshService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

/**
 * @author bwcx_jzy
 * @date 2019/8/17
 */
@Controller
@RequestMapping(value = "node/ssh")
@Feature(cls = ClassFeature.SSH)
public class SshEditController extends BaseServerController {

	@Resource
	private SshService sshService;

	@Resource
	private BuildInfoService buildInfoService;

	@RequestMapping(value = "del.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@Feature(method = MethodFeature.DEL)
	public String del(@ValidatorItem(value = ValidatorRule.NOT_BLANK) String id) {
		boolean checkSsh = buildInfoService.checkReleaseMethod(id, BuildReleaseMethod.Ssh);
		Assert.state(!checkSsh, "当前ssh存在构建项，不能删除");
		sshService.deleteItem(id);
		return JsonMessage.getString(200, "操作成功");
	}
}
