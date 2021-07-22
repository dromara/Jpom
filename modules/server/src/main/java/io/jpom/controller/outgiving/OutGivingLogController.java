package io.jpom.controller.outgiving;

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
import io.jpom.model.log.OutGivingLog;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.dblog.DbOutGivingLogService;
import io.jpom.service.node.OutGivingServer;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

/**
 * 分发日志
 *
 * @author bwcx_jzy
 * @date 2019/7/20
 */
@Controller
@RequestMapping(value = "/outgiving")
@Feature(cls = ClassFeature.OUTGIVING)
public class OutGivingLogController extends BaseServerController {
    @Resource
    private OutGivingServer outGivingServer;
    @Resource
    private DbOutGivingLogService dbOutGivingLogService;

//    @RequestMapping(value = "log.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
//    @Feature(method = MethodFeature.LOG)
//    public String list() throws IOException {
//        // 所有节点
//        List<NodeModel> nodeModels = nodeService.list();
//        setAttribute("nodeArray", nodeModels);
//        //
//        List<OutGivingModel> outGivingModels = outGivingServer.list();
//        setAttribute("outGivingModels", outGivingModels);
//        //
//        JSONArray status = BaseEnum.toJSONArray(OutGivingNodeProject.Status.class);
//        setAttribute("status", status);
//        return "outgiving/loglist";
//    }

    @RequestMapping(value = "log_list_data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @Feature(method = MethodFeature.LOG)
    public String listData(String nodeId,
                           String outGivingId,
                           String status,
                           @ValidatorConfig(value = {
                                   @ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "limit error")
                           }, defaultVal = "10") int limit,
                           @ValidatorConfig(value = {
                                   @ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "page error")
                           }, defaultVal = "1") int page) {

        Page pageObj = new Page(page, limit);
        Entity entity = Entity.create();
        this.doPage(pageObj, entity, "startTime");
        if (StrUtil.isNotEmpty(nodeId)) {
            entity.set("nodeId", nodeId);
        }

        if (StrUtil.isNotEmpty(outGivingId)) {
            entity.set("outGivingId", outGivingId);
        }

        if (StrUtil.isNotEmpty(status)) {
            entity.set("outGivingId", status);
        }

        PageResult<OutGivingLog> pageResult = dbOutGivingLogService.listPage(entity, pageObj);
        JSONObject jsonObject = JsonMessage.toJson(200, "获取成功", pageResult);
        jsonObject.put("total", pageResult.getTotal());
        return jsonObject.toString();
    }
}
