package cn.keepbx.jpom.controller.node;

import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseController;
import cn.keepbx.jpom.common.commander.AbstractSystemCommander;
import cn.keepbx.jpom.model.system.ProcessModel;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * 欢迎页
 *
 * @author Administrator
 */
@Controller
@RequestMapping(value = "/node")
public class NodeWelcomeController extends BaseController {

    @RequestMapping(value = "welcome", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String welcome() {
        return "node/welcome";
    }

    @RequestMapping(value = "getTop", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String getTop() {
        JSONObject topInfo = AbstractSystemCommander.getInstance().getAllMonitor();
        return JsonMessage.getString(200, "", topInfo);
    }

    @RequestMapping(value = "processList", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String getProcessList() {
        List<ProcessModel> array = AbstractSystemCommander.getInstance().getProcessList();
        return JsonMessage.getString(200, "", array);
    }
}
