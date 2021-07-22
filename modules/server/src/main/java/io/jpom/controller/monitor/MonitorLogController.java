package io.jpom.controller.monitor;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.Page;
import cn.hutool.db.PageResult;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorConfig;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.model.log.MonitorNotifyLog;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.dblog.DbMonitorNotifyLogService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

/**
 * 监控列表
 *
 * @author bwcx_jzy
 * @date 2019/7/16
 */
@Controller
@RequestMapping(value = "/monitor")
@Feature(cls = ClassFeature.MONITOR)
public class MonitorLogController extends BaseServerController {

    @Resource
    private DbMonitorNotifyLogService dbMonitorNotifyLogService;

//    /**
//     * 展示监控页面
//     *
//     * @return page
//     */
//    @RequestMapping(value = "log.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
//    @Feature(method = MethodFeature.LOG)
//    public String list() {
//        // 所有节点
//        List<NodeModel> nodeModels = nodeService.list();
//        setAttribute("nodeArray", nodeModels);
//
//        //通知方式
//        JSONArray notifyTypeArray = BaseEnum.toJSONArray(MonitorModel.NotifyType.class);
//        setAttribute("notifyTypeArray", notifyTypeArray);
//        return "monitor/loglist";
//    }

    /**
     * 展示用户列表
     *
     * @param selectNode   节点
     * @param limit        限制
     * @param notifyStatus 状态
     * @return json
     */
    @RequestMapping(value = "list_data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @Feature(method = MethodFeature.LOG)
    public String listData(String selectNode, String notifyStatus,
                           @ValidatorConfig(value = {
                                   @ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "limit error")
                           }, defaultVal = "10") int limit,
                           @ValidatorConfig(value = {
                                   @ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "page error")
                           }, defaultVal = "1") int page) {
        Page pageObj = new Page(page, limit);
        Entity entity = Entity.create();
        this.doPage(pageObj, entity, "createTime");

        if (StrUtil.isNotEmpty(selectNode)) {
            entity.set("nodeId", selectNode);
        }

        if (StrUtil.isNotEmpty(notifyStatus)) {
            entity.set("notifyStatus", Convert.toBool(notifyStatus, true));
        }

        PageResult<MonitorNotifyLog> pageResult = dbMonitorNotifyLogService.listPage(entity, pageObj);
        JSONObject jsonObject = JsonMessage.toJson(200, "获取成功", pageResult);
        jsonObject.put("total", pageResult.getTotal());
        return jsonObject.toString();
    }
}
