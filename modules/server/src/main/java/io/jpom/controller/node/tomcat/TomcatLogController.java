package io.jpom.controller.node.tomcat;

import io.jpom.common.BaseServerController;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
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

//    /**
//     * tomcat 日志管理
//     *
//     * @param id tomcat id
//     * @return 项目管理面
//     */
//    @RequestMapping(value = "console", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
//    @Feature(method = MethodFeature.LOG)
//    public String console(String id) {
//        setAttribute("id", id);
//        return "node/tomcat/console";
//    }

    @RequestMapping(value = "getLogList", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @Feature(method = MethodFeature.LOG)
    public String getLogList() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.Tomcat_LOG_List).toString();
    }
}
