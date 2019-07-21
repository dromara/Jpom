package cn.keepbx.jpom.controller.system;

import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.common.forward.NodeForward;
import cn.keepbx.jpom.common.forward.NodeUrl;
import cn.keepbx.jpom.system.WebAopLog;
import cn.keepbx.util.LayuiTreeUtil;
import com.alibaba.fastjson.JSONArray;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 系统日志管理
 *
 * @author bwcx_jzy
 * @date 2019/7/20
 */
@Controller
@RequestMapping(value = "system")
public class LogManageController extends BaseServerController {

    @RequestMapping(value = "log.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String log() {
        return "system/log";
    }

    @RequestMapping(value = "log_data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String logData(String nodeId) {
        if (StrUtil.isNotEmpty(nodeId)) {
            return NodeForward.request(getNode(), getRequest(), NodeUrl.SystemLog).toString();
        }
        WebAopLog webAopLog = SpringUtil.getBean(WebAopLog.class);
        JSONArray data = LayuiTreeUtil.getTreeData(webAopLog.getPropertyValue());
        return JsonMessage.getString(200, "", data);
    }
}
