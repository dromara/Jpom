package cn.keepbx.jpom.controller;

import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseAgentController;
import cn.keepbx.jpom.model.system.JpomManifest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author jiangzeyin
 * @date 2019/4/17
 */
@RestController
public class IndexController extends BaseAgentController {

    @RequestMapping(value = {"index", "", "index.html", "/"}, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String index() {
        return "Jpom-Agent";
    }

    @RequestMapping(value = "info", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String info() {
        return JsonMessage.getString(200, "", JpomManifest.getInstance());
    }
}
