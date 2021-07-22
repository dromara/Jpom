package io.jpom.controller.monitor;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorConfig;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.common.interceptor.OptLog;
import io.jpom.model.Cycle;
import io.jpom.model.data.MonitorModel;
import io.jpom.model.data.UserModel;
import io.jpom.model.log.UserOperateLogV1;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.dblog.DbMonitorNotifyLogService;
import io.jpom.service.monitor.MonitorService;
import io.jpom.service.user.UserService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 监控列表
 *
 * @author bwcx_jzy
 * @date 2019/6/15
 */
@Controller
@RequestMapping(value = "/monitor")
@Feature(cls = ClassFeature.MONITOR)
public class MonitorListController extends BaseServerController {

    @Resource
    private MonitorService monitorService;

    @Resource
    private DbMonitorNotifyLogService dbMonitorNotifyLogService;

    @Resource
    private UserService userService;

//    /**
//     * 展示监控页面
//     *
//     * @return page
//     */
//    @RequestMapping(value = "list.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
//    @Feature(method = MethodFeature.LIST)
//    public String list() {
//        return "monitor/list";
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
//        MonitorModel monitorModel = null;
//        if (StrUtil.isNotEmpty(id)) {
//            monitorModel = monitorService.getItem(id);
//        }
//        setAttribute("model", monitorModel);
//        //监控周期
//        JSONArray cycleArray = Cycle.getJSONArray();
//        setAttribute("cycleArray", cycleArray);
//        List<NodeModel> nodeModels = nodeService.listAndProject();
//        setAttribute("nodeModels", nodeModels);
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
//        setAttribute("userLists", jsonArray);
//        return "monitor/edit";
//    }

    /**
     * 展示监控列表
     *
     * @return json
     */
    @RequestMapping(value = "getMonitorList", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @Feature(method = MethodFeature.LIST)
    public String getMonitorList() {
        List<MonitorModel> list = monitorService.list();
        return JsonMessage.getString(200, "", list);
    }

    /**
     * 删除列表
     *
     * @param id id
     * @return json
     */
    @RequestMapping(value = "deleteMonitor", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @OptLog(UserOperateLogV1.OptType.DelMonitor)
    @Feature(method = MethodFeature.DEL)
    public String deleteMonitor(@ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "删除失败")) String id) throws SQLException {
        // 删除日志
        Entity where = new Entity();
        where.set("monitorId", id);
        dbMonitorNotifyLogService.del(where);
        //
        monitorService.deleteItem(id);
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
    @RequestMapping(value = "updateMonitor", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @OptLog(UserOperateLogV1.OptType.EditMonitor)
    @Feature(method = MethodFeature.EDIT)
    public String updateMonitor(String id,
                                @ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "监控名称不能为空")) String name,
                                String notifyUser) {
        int cycle = getParameterInt("cycle", Cycle.five.getCode());
        String status = getParameter("status");
        String autoRestart = getParameter("autoRestart");

        JSONArray jsonArray = JSONArray.parseArray(notifyUser);
        List<String> notifyUsers = jsonArray.toJavaList(String.class);
        if (notifyUsers == null || notifyUsers.isEmpty()) {
            return JsonMessage.getString(405, "请选择报警联系人");
        }
        String projects = getParameter("projects");
        JSONArray projectsArray = JSONArray.parseArray(projects);
        if (projectsArray == null || projectsArray.size() <= 0) {
            return JsonMessage.getString(400, "请至少选择一个项目");
        }
        boolean start = "on".equalsIgnoreCase(status);
        MonitorModel monitorModel = monitorService.getItem(id);
        if (monitorModel == null) {
            monitorModel = new MonitorModel();
        }
        //
        List<MonitorModel.NodeProject> nodeProjects = new ArrayList<>();
        projectsArray.forEach(o -> {
            JSONObject jsonObject = (JSONObject) o;
            nodeProjects.add(jsonObject.toJavaObject(MonitorModel.NodeProject.class));
        });
        monitorModel.setAutoRestart("on".equalsIgnoreCase(autoRestart));
        monitorModel.setCycle(cycle);
        monitorModel.setProjects(nodeProjects);
        monitorModel.setStatus(start);
        monitorModel.setNotifyUser(notifyUsers);
        monitorModel.setName(name);

        if (StrUtil.isEmpty(id)) {
            //添加监控
            id = IdUtil.objectId();
            UserModel user = getUser();
            monitorModel.setId(id);
            monitorModel.setParent(UserModel.getOptUserName(user));
            monitorService.addItem(monitorModel);
            return JsonMessage.getString(200, "添加成功");
        }
        monitorService.updateItem(monitorModel);
        return JsonMessage.getString(200, "修改成功");
    }

    /**
     * 开启或关闭监控
     *
     * @param id     id
     * @param status 状态
     * @param type   类型
     * @return json
     */
    @RequestMapping(value = "changeStatus", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @OptLog(UserOperateLogV1.OptType.ChangeStatusMonitor)
    @Feature(method = MethodFeature.EDIT)
    public String changeStatus(@ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "id不能为空")) String id,
                               String status, String type) {
        MonitorModel monitorModel = monitorService.getItem(id);
        if (monitorModel == null) {
            return JsonMessage.getString(405, "不存在监控项啦");
        }
        boolean bStatus = Convert.toBool(status, false);
        if ("status".equalsIgnoreCase(type)) {
            monitorModel.setStatus(bStatus);
        } else if ("restart".equalsIgnoreCase(type)) {
            monitorModel.setAutoRestart(bStatus);
        } else {
            return JsonMessage.getString(405, "type不正确");
        }
        monitorService.updateItem(monitorModel);
        return JsonMessage.getString(200, "修改成功");
    }


}
