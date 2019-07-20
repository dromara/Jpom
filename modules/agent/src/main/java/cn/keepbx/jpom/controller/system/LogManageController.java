package cn.keepbx.jpom.controller.system;

import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.jpom.common.BaseAgentController;
import cn.keepbx.jpom.system.WebAopLog;
import com.alibaba.fastjson.JSONArray;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统日志管理
 *
 * @author bwcx_jzy
 * @date 2019/7/20
 */
@RestController
@RequestMapping(value = "system")
public class LogManageController extends BaseAgentController {


    @RequestMapping(value = "log_data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String logData() {
        WebAopLog webAopLog = SpringUtil.getBean(WebAopLog.class);
        JSONArray data = webAopLog.getTreeData();
        return JsonMessage.getString(200, "", data);
    }
}
