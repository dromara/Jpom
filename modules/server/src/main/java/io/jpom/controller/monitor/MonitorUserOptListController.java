package io.jpom.controller.monitor;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorConfig;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.common.interceptor.OptLog;
import io.jpom.model.data.MonitorUserOptModel;
import io.jpom.model.data.UserModel;
import io.jpom.model.log.UserOperateLogV1;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.monitor.MonitorUserOptService;
import io.jpom.service.user.UserService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.List;

/**
 * 监控用户操作
 *
 * @author bwcx_jzy
 * @date 2020/08/06
 */
@Controller
@RequestMapping(value = "/monitor_user_opt")
@Feature(cls = ClassFeature.MONITOR)
public class MonitorUserOptListController extends BaseServerController {

    @Resource
    private MonitorUserOptService monitorUserOptService;

    @Resource
    private UserService userService;

//    /**
//     * 展示监控页面
//     *
//     * @return page
//     */
//    @RequestMapping(value = "userOpt.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
//    @Feature(method = MethodFeature.LIST)
//    public String list() {
//        return "monitor/userOpt";
//    }

//    /**
//     * 修改监控
//     *
//     * @param id id
//     * @return json
//     */
//    @RequestMapping(value = "edit.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
//    @Feature(method = MethodFeature.EDIT)
//    public String edit(String id) {
//        MonitorUserOptModel monitorModel = null;
//        if (StrUtil.isNotEmpty(id)) {
//            monitorModel = monitorUserOptService.getItem(id);
//        }
//        setAttribute("model", monitorModel);
//        //
//        List<UserModel> list = userService.list(false);
//        JSONArray jsonArray = new JSONArray();
//        list.forEach(userModel -> {
//            JSONObject jsonObject = new JSONObject();
//            jsonObject.put("title", userModel.getName());
//            jsonObject.put("value", userModel.getId());
//            if (StrUtil.isEmpty(userModel.getEmail()) && StrUtil.isEmpty(userModel.getDingDing())) {
//                jsonObject.put("disabled", true);
//            }
//            jsonArray.add(jsonObject);
//        });
//        UserOperateLogV1.OptType[] values = UserOperateLogV1.OptType.values();
//        JSONArray jsonArrayOpt = new JSONArray();
//        for (UserOperateLogV1.OptType value : values) {
//            JSONObject jsonObject = new JSONObject();
//            jsonObject.put("title", value.getDesc());
//            jsonObject.put("value", value.name());
//            jsonArrayOpt.add(jsonObject);
//        }
//        setAttribute("opts", jsonArrayOpt);
//        setAttribute("userLists", jsonArray);
//        return "monitor/edit-user-opt";
//    }

    @RequestMapping(value = "list_data", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @Feature(method = MethodFeature.LIST)
    public String getMonitorList() {
        List<MonitorUserOptModel> list = monitorUserOptService.list();
        return JsonMessage.getString(200, "", list);
    }

    /**
     * 操作监控类型列表
     * @return json
     */
    @RequestMapping(value = "type_data", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @Feature(method = MethodFeature.LIST)
    public String getOperateTypeList() {
        UserOperateLogV1.OptType[] values = UserOperateLogV1.OptType.values();
        JSONArray jsonArrayOpt = new JSONArray();
        for (UserOperateLogV1.OptType value : values) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("title", value.getDesc());
            jsonObject.put("value", value.name());
            jsonArrayOpt.add(jsonObject);
        }
        return JsonMessage.getString(200, "success", jsonArrayOpt);
    }

    /**
     * 删除列表
     *
     * @param id id
     * @return json
     */
    @RequestMapping(value = "delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @OptLog(UserOperateLogV1.OptType.DelMonitor)
    @Feature(method = MethodFeature.DEL)
    public String deleteMonitor(@ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "删除失败")) String id) {
        //
        monitorUserOptService.deleteItem(id);
        return JsonMessage.getString(200, "删除成功");
    }


    /**
     * 增加或修改监控
     *
     * @param id         id
     * @param name       name
     * @param notifyUser user
     * @return json
     */
    @RequestMapping(value = "update", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @OptLog(UserOperateLogV1.OptType.EditMonitor)
    @Feature(method = MethodFeature.EDIT)
    public String updateMonitor(String id,
                                @ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "监控名称不能为空")) String name,
                                String notifyUser, String monitorUser, String monitorOpt) {

        String status = getParameter("status");

        JSONArray jsonArray = JSONArray.parseArray(notifyUser);
        List<String> notifyUsers = jsonArray.toJavaList(String.class);
        if (CollUtil.isEmpty(notifyUsers)) {
            return JsonMessage.getString(405, "请选择报警联系人");
        }

        JSONArray monitorUserArray = JSONArray.parseArray(monitorUser);
        List<String> monitorUserArrays = monitorUserArray.toJavaList(String.class);
        if (CollUtil.isEmpty(monitorUserArrays)) {
            return JsonMessage.getString(405, "请选择监控人员");
        }


        JSONArray monitorOptArray = JSONArray.parseArray(monitorOpt);
        List<UserOperateLogV1.OptType> monitorOptArrays = monitorOptArray.toJavaList(UserOperateLogV1.OptType.class);

        if (CollUtil.isEmpty(monitorOptArrays)) {
            return JsonMessage.getString(405, "请选择监控的操作");
        }


        boolean start = "on".equalsIgnoreCase(status);
        MonitorUserOptModel monitorModel = monitorUserOptService.getItem(id);
        if (monitorModel == null) {
            monitorModel = new MonitorUserOptModel();
        }
        monitorModel.setMonitorUser(monitorUserArrays);
        monitorModel.setStatus(start);
        monitorModel.setMonitorOpt(monitorOptArrays);
        monitorModel.setNotifyUser(notifyUsers);
        monitorModel.setName(name);

        if (StrUtil.isEmpty(id)) {
            //添加监控
            id = IdUtil.objectId();
            UserModel user = getUser();
            monitorModel.setId(id);
            monitorModel.setParent(UserModel.getOptUserName(user));
            monitorUserOptService.addItem(monitorModel);
            return JsonMessage.getString(200, "添加成功");
        }
        monitorUserOptService.updateItem(monitorModel);
        return JsonMessage.getString(200, "修改成功");
    }

    /**
     * 开启或关闭监控
     *
     * @param id     id
     * @param status 状态
     * @return json
     */
    @RequestMapping(value = "changeStatus", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @OptLog(UserOperateLogV1.OptType.ChangeStatusMonitor)
    @Feature(method = MethodFeature.EDIT)
    public String changeStatus(@ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "id不能为空")) String id,
                               String status) {
        MonitorUserOptModel monitorModel = monitorUserOptService.getItem(id);
        if (monitorModel == null) {
            return JsonMessage.getString(405, "不存在监控项啦");
        }
        boolean bStatus = Convert.toBool(status, false);
        monitorModel.setStatus(bStatus);
        monitorUserOptService.updateItem(monitorModel);
        return JsonMessage.getString(200, "修改成功");
    }


}
