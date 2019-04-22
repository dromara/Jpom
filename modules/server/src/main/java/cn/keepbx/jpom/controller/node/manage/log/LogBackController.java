package cn.keepbx.jpom.controller.node.manage.log;

import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.common.forward.NodeForward;
import cn.keepbx.jpom.common.forward.NodeUrl;
import cn.keepbx.jpom.common.interceptor.ProjectPermission;
import cn.keepbx.jpom.model.data.UserOperateLogV1;
import cn.keepbx.jpom.service.manage.ProjectInfoService;
import cn.keepbx.jpom.system.OperateType;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

/**
 * 控制台日志备份管理
 *
 * @author jiangzeyin
 * @date 2019/3/7
 */
@Controller
@RequestMapping(value = "node/manage/log")
public class LogBackController extends BaseServerController {
    @Resource
    private ProjectInfoService projectInfoService;

    @RequestMapping(value = "export.html", method = RequestMethod.GET)
    @ResponseBody
    @ProjectPermission
    @OperateType(UserOperateLogV1.OptType.ExportProjectLog)
    public void export() {
        NodeForward.requestDownload(getNode(), getRequest(), getResponse(), NodeUrl.Manage_Log_export);
    }

    @RequestMapping(value = "logBack", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String console(String id) {
        JSONObject jsonObject = NodeForward.requestData(getNode(), NodeUrl.Manage_Log_logBack, getRequest(), JSONObject.class);
        setAttribute("data", jsonObject);
        return "node/manage/logBack";
    }

    @RequestMapping(value = "logBack_download", method = RequestMethod.GET)
    @ResponseBody
    @ProjectPermission
    @OperateType(UserOperateLogV1.OptType.DownloadProjectLogBack)
    public void download() {
        NodeForward.requestDownload(getNode(), getRequest(), getResponse(), NodeUrl.Manage_Log_logBack_download);
    }

    @RequestMapping(value = "logBack_delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @ProjectPermission(checkDelete = true)
    @OperateType(UserOperateLogV1.OptType.DelProjectLogBack)
    public String clear() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.Manage_Log_logBack_delete).toString();
    }

    @RequestMapping(value = "logSize", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String logSize(String id) {
        JSONObject info = projectInfoService.getLogSize(getNode(), id);
        return JsonMessage.getString(200, "", info);
    }

    /**
     * 重置日志
     *
     * @return json
     */
    @RequestMapping(value = "resetLog", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @ProjectPermission
    @OperateType(UserOperateLogV1.OptType.RestProjectLog)
    public String resetLog() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.Manage_Log_ResetLog).toString();
    }
}
