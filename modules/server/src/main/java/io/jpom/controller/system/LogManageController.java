package io.jpom.controller.system;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import com.alibaba.fastjson.JSONArray;
import io.jpom.common.BaseServerController;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.common.interceptor.OptLog;
import io.jpom.model.log.UserOperateLogV1;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.socket.ServiceFileTailWatcher;
import io.jpom.system.WebAopLog;
import io.jpom.util.LayuiTreeUtil;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * 系统日志管理
 *
 * @author bwcx_jzy
 * @date 2019/7/20
 */
@Controller
@RequestMapping(value = "system")
@Feature(cls = ClassFeature.SYSTEM)
public class LogManageController extends BaseServerController {

//    @RequestMapping(value = "log.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
//    @Feature(method = MethodFeature.LOG)
//    public String log() {
//        return "system/log";
//    }

    @RequestMapping(value = "log_data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @Feature(method = MethodFeature.LOG)
    public String logData(String nodeId) {
        if (StrUtil.isNotEmpty(nodeId)) {
            return NodeForward.request(getNode(), getRequest(), NodeUrl.SystemLog).toString();
        }
        WebAopLog webAopLog = SpringUtil.getBean(WebAopLog.class);
        JSONArray data = LayuiTreeUtil.getTreeData(webAopLog.getPropertyValue());
        return JsonMessage.getString(200, "", data);
    }

    /**
     * 删除 需要验证是否最后修改时间
     *
     * @param nodeId 节点
     * @param path   路径
     * @return json
     */
    @RequestMapping(value = "log_del.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @OptLog(UserOperateLogV1.OptType.DelSysLog)
    @Feature(method = MethodFeature.DEL_LOG)
    public String logData(String nodeId,
                          @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "path错误") String path) {
        if (StrUtil.isNotEmpty(nodeId)) {
            return NodeForward.request(getNode(), getRequest(), NodeUrl.DelSystemLog).toString();
        }
        WebAopLog webAopLog = SpringUtil.getBean(WebAopLog.class);
        File file = FileUtil.file(webAopLog.getPropertyValue(), path);
        // 判断修改时间
        long modified = file.lastModified();
        if (System.currentTimeMillis() - modified < TimeUnit.DAYS.toMillis(1)) {
            return JsonMessage.getString(405, "不能删除当天的日志");
        }
        if (FileUtil.del(file)) {
            // 离线上一个日志
            ServiceFileTailWatcher.offlineFile(file);
            return JsonMessage.getString(200, "删除成功");
        }
        return JsonMessage.getString(500, "删除失败");
    }


    @RequestMapping(value = "log_download", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @Feature(method = MethodFeature.DOWNLOAD)
    public void logDownload(String nodeId,
                            @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "path错误") String path) {
        if (StrUtil.isNotEmpty(nodeId)) {
            NodeForward.requestDownload(getNode(), getRequest(), getResponse(), NodeUrl.DownloadSystemLog);
            return;
        }
        WebAopLog webAopLog = SpringUtil.getBean(WebAopLog.class);
        File file = FileUtil.file(webAopLog.getPropertyValue(), path);
        if (file.isFile()) {
            ServletUtil.write(getResponse(), file);
        }
    }
}
