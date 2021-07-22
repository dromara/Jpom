package io.jpom.controller.node.manage;

import io.jpom.common.BaseServerController;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(value = "/node/manage/")
@Feature(cls = ClassFeature.PROJECT)
public class JdkManageController extends BaseServerController {

//    /**
//     * jdk管理
//     *
//     * @return page
//     */
//    @RequestMapping(value = "jdkList.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
//    public String listHtml() {
//        return "node/manage/JdkList";
//    }

    @RequestMapping(value = "jdk/list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String list() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.Manage_jdk_list).toString();
    }

    @RequestMapping(value = "jdk/delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String delete() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.Manage_jdk_delete).toString();
    }

    @RequestMapping(value = "jdk/update", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String update() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.Manage_jdk_update).toString();
    }
}
