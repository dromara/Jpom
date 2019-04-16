package cn.keepbx.jpom.controller;

import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.controller.base.AbstractController;
import cn.keepbx.jpom.common.commander.AbstractSystemCommander;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author jiangzeyin
 * @date 2019/4/16
 */
@RestController
public class WelcomeController extends AbstractController {

    @RequestMapping(value = "getTop", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getTop() {
        JSONObject topInfo = AbstractSystemCommander.getInstance().getAllMonitor();
        return JsonMessage.getString(200, "", topInfo);
    }
}
