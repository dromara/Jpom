package cn.keepbx.jpom.controller.monitor;

import cn.keepbx.jpom.common.BaseServerController;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * 监控列表
 *
 * @author bwcx_jzy
 * @date 2019/6/15
 */
@Controller
@RequestMapping(value = "/monitor")
public class MonitorListController extends BaseServerController {

    /**
     * 展示用户列表
     */
    @RequestMapping(value = "list.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String list() {
        return "monitor/list";
    }
}
