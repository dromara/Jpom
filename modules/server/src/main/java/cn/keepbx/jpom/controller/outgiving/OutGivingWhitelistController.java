package cn.keepbx.jpom.controller.outgiving;

import cn.hutool.core.text.StrSpliter;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.common.interceptor.OptLog;
import cn.keepbx.jpom.model.data.AgentWhitelist;
import cn.keepbx.jpom.model.data.ServerWhitelist;
import cn.keepbx.jpom.model.data.UserModel;
import cn.keepbx.jpom.model.log.UserOperateLogV1;
import cn.keepbx.jpom.service.system.ServerWhitelistServer;
import cn.keepbx.permission.SystemPermission;
import cn.keepbx.plugin.ClassFeature;
import cn.keepbx.plugin.Feature;
import cn.keepbx.plugin.MethodFeature;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.List;

/**
 * 节点白名单
 *
 * @author jiangzeyin
 * @date 2019/4/22
 */
@Controller
@RequestMapping(value = "/outgiving")
@Feature(cls = ClassFeature.OUTGIVING)
public class OutGivingWhitelistController extends BaseServerController {
    @Resource
    private ServerWhitelistServer serverWhitelistServer;


    @RequestMapping(value = "whitelistDirectory.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    @SystemPermission
    public String whitelistDirectory() {
        //
        UserModel userModel = getUser();
        ServerWhitelist serverWhitelist = serverWhitelistServer.getWhitelist();
        if (serverWhitelist != null && userModel.isSystemUser()) {
            List<String> whiteList = serverWhitelist.getOutGiving();
            String strWhiteList = AgentWhitelist.convertToLine(whiteList);
            setAttribute("whiteList", strWhiteList);
        }
        return "outgiving/whitelistDirectory";
    }

    /**
     * 保存节点白名单
     *
     * @param data 数据
     * @return json
     */
    @RequestMapping(value = "whitelistDirectory_submit", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @OptLog(UserOperateLogV1.OptType.SaveOutgivingWhitelist)
    @SystemPermission
    public String whitelistDirectorySubmit(String data) {
        List<String> list = StrSpliter.splitTrim(data, StrUtil.LF, true);
        if (list == null || list.size() <= 0) {
            return JsonMessage.getString(401, "白名单不能为空");
        }
        list = AgentWhitelist.covertToArray(list);
        if (list == null) {
            return JsonMessage.getString(401, "项目路径白名单不能位于Jpom目录下");
        }
        if (list.isEmpty()) {
            return JsonMessage.getString(401, "项目路径白名单不能为空");
        }
        ServerWhitelist serverWhitelist = serverWhitelistServer.getWhitelist();
        if (serverWhitelist == null) {
            serverWhitelist = new ServerWhitelist();
        }
        serverWhitelist.setOutGiving(list);
        serverWhitelistServer.saveWhitelistDirectory(serverWhitelist);

        String resultData = AgentWhitelist.convertToLine(list);
        return JsonMessage.getString(200, "保存成功", resultData);
    }
}
