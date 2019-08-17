package cn.keepbx.jpom.controller.system;

import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.common.interceptor.OptLog;
import cn.keepbx.jpom.model.data.MailAccountModel;
import cn.keepbx.jpom.model.data.UserModel;
import cn.keepbx.jpom.model.log.UserOperateLogV1;
import cn.keepbx.jpom.service.system.SystemMailConfigService;
import cn.keepbx.permission.SystemPermission;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

/**
 * 监控邮箱配置
 *
 * @author bwcx_jzy
 * @date 2019/7/16
 */
@Controller
@RequestMapping(value = "system")
public class SystemMailConfigController extends BaseServerController {

    @Resource
    private SystemMailConfigService systemMailConfigService;

    /**
     * 展示监控页面
     */
    @RequestMapping(value = "mailConfig.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    @SystemPermission
    public String mailConfig() {
        UserModel userModel = getUser();
        if (userModel.isSystemUser()) {
            MailAccountModel item = systemMailConfigService.getConfig();
            setAttribute("item", item);
        }
        return "monitor/mailConfig";
    }

    @RequestMapping(value = "mailConfig_save.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @OptLog(UserOperateLogV1.OptType.EditMailConfig)
    @SystemPermission
    public String listData(MailAccountModel mailAccountModel) {
        if (mailAccountModel == null) {
            return JsonMessage.getString(405, "请填写信息");
        }
        if (StrUtil.isBlank(mailAccountModel.getHost())) {
            return JsonMessage.getString(405, "请填写host");
        }
        if (StrUtil.isBlank(mailAccountModel.getUser())) {
            return JsonMessage.getString(405, "请填写user");
        }
        if (StrUtil.isBlank(mailAccountModel.getPass())) {
            return JsonMessage.getString(405, "请填写pass");
        }
        if (StrUtil.isBlank(mailAccountModel.getFrom())) {
            return JsonMessage.getString(405, "请填写from");
        }
        systemMailConfigService.save(mailAccountModel);
        return JsonMessage.getString(200, "保存成功");
    }
}
