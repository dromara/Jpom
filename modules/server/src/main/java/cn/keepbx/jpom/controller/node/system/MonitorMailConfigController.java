package cn.keepbx.jpom.controller.node.system;

import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.common.interceptor.OptLog;
import cn.keepbx.jpom.model.data.MailAccountModel;
import cn.keepbx.jpom.model.data.UserModel;
import cn.keepbx.jpom.model.log.UserOperateLogV1;
import cn.keepbx.jpom.service.monitor.MonitorMailConfigService;
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
@RequestMapping(value = "/monitor")
public class MonitorMailConfigController extends BaseServerController {

    @Resource
    private MonitorMailConfigService monitorMailConfigService;

    /**
     * 展示监控页面
     */
    @RequestMapping(value = "mailConfig.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String mailConfig() {
        UserModel userModel = getUserModel();
        if (userModel.isSystemUser()) {
            MailAccountModel item = monitorMailConfigService.getConfig();
            setAttribute("item", item);
        }
        return "monitor/mailConfig";
    }

    @RequestMapping(value = "mailConfig_save.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @OptLog(UserOperateLogV1.OptType.EditMailConfig)
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
        monitorMailConfigService.save(mailAccountModel);
        return JsonMessage.getString(200, "保存成功");
    }
}
