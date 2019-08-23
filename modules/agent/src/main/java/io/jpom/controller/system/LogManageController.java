package io.jpom.controller.system;

import cn.hutool.core.io.FileUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import com.alibaba.fastjson.JSONArray;
import io.jpom.common.BaseAgentController;
import io.jpom.system.WebAopLog;
import io.jpom.util.LayuiTreeUtil;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.concurrent.TimeUnit;

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
        JSONArray data = LayuiTreeUtil.getTreeData(webAopLog.getPropertyValue());
        return JsonMessage.getString(200, "", data);
    }


    @RequestMapping(value = "log_del.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String logData(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "path错误") String path) {
        WebAopLog webAopLog = SpringUtil.getBean(WebAopLog.class);
        File file = FileUtil.file(webAopLog.getPropertyValue(), path);
        // 判断修改时间
        long modified = file.lastModified();
        if (System.currentTimeMillis() - modified < TimeUnit.DAYS.toMillis(1)) {
            return JsonMessage.getString(405, "不能删除当天的日志");
        }
        if (FileUtil.del(file)) {
            return JsonMessage.getString(200, "删除成功");
        }
        return JsonMessage.getString(500, "删除失败");
    }


    @RequestMapping(value = "log_download", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public void logDownload(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "path错误") String path) {
        WebAopLog webAopLog = SpringUtil.getBean(WebAopLog.class);
        File file = FileUtil.file(webAopLog.getPropertyValue(), path);
        if (file.isFile()) {
            ServletUtil.write(getResponse(), file);
        }
    }
}
