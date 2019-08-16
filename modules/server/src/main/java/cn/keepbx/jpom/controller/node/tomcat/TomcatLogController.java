package cn.keepbx.jpom.controller.node.tomcat;

import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.common.forward.NodeForward;
import cn.keepbx.jpom.common.forward.NodeUrl;
import cn.keepbx.plugin.ClassFeature;
import cn.keepbx.plugin.Feature;
import cn.keepbx.plugin.MethodFeature;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author bwcx_jzy
 * @date 2019/8/16
 */
@Controller
@RequestMapping(value = TomcatManageController.TOMCAT_URL)
@Feature(cls = ClassFeature.TOMCAT)
public class TomcatLogController extends BaseServerController {

    /**
     * tomcat 日志管理
     *
     * @param id tomcat id
     * @return 项目管理面
     */
    @RequestMapping(value = "console", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    @Feature(method = MethodFeature.LOG)
    public String console(String id) {
        setAttribute("id", id);
        return "node/tomcat/console";
    }

    @RequestMapping(value = "getLogList", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @Feature(method = MethodFeature.LOG)
    public String getLogList() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.Tomcat_LOG_List).toString();
    }
}
