package cn.keepbx.jpom.controller.monitor;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.model.BaseEnum;
import cn.keepbx.jpom.model.data.MonitorModel;
import cn.keepbx.jpom.model.data.NodeModel;
import cn.keepbx.jpom.model.data.UserModel;
import cn.keepbx.jpom.service.monitor.MonitorService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * 监控列表
 *
 * @author bwcx_jzy
 * @date 2019/6/15
 */
@Controller
@RequestMapping(value = "/monitor")
public class MonitorListController extends BaseServerController {

    @Resource
    private MonitorService monitorService;

    /**
     * 展示监控页面
     */
    @RequestMapping(value = "list.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String list() {
        List<MonitorModel> list = monitorService.list();
        setAttribute("list", list);
        return "monitor/list";
    }

    /**
     * 修改监控
     */
    @RequestMapping(value = "edit.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String edit(String id) {
        MonitorModel monitorModel = null;
        if (StrUtil.isNotEmpty(id)) {
            monitorModel = monitorService.getItem(id);
        }
        setAttribute("model", monitorModel);
        //监控周期
        JSONArray cycleArray = BaseEnum.toJSONArray(MonitorModel.Cycle.class);
        setAttribute("cycleArray", cycleArray);
        //通知方式
        JSONArray notifyTypeArray = BaseEnum.toJSONArray(MonitorModel.NotifyType.class);
        setAttribute("notifyTypeArray", notifyTypeArray);
        List<NodeModel> nodeModels = nodeService.listAndProject();
        setAttribute("nodeModels", nodeModels);
        return "monitor/edit";
    }

    /**
     * 展示监控列表
     */
    @RequestMapping(value = "getMonitorList", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String getMonitorList() {
        List<MonitorModel> list = monitorService.list();
        return JsonMessage.getString(200, "", list);
    }

    /**
     * 删除列表
     */
    @RequestMapping(value = "deleteMonitor", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String deleteMonitor(String id) {
        if (StrUtil.isEmpty(id)) {
            return JsonMessage.getString(400, "删除失败");
        }
        boolean b = monitorService.checkMonitorRunning(id);
        if (b) {
            return JsonMessage.getString(400, "不能删除正在运行的监控");
        }
        monitorService.deleteItem(id);
        return JsonMessage.getString(200, "删除成功");
    }

    /**
     * 增加或修改监控
     */
    @RequestMapping(value = "updateMonitor", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String updateMonitor() {
        String id = getParameter("id");
        String notify = getParameter("notify");
        String name = getParameter("name");
        int cycle = getParameterInt("cycle", 30);
        String status = getParameter("status");
        String autoRestart = getParameter("autoRestart");
        JSONArray notifyArray = JSONArray.parseArray(notify);
        if (notify == null || notifyArray.size() <= 0) {
            return JsonMessage.getString(400, "请至少选择一种通知方式");
        }
        List<MonitorModel.Notify> notifies = new ArrayList<>();
        for (int i = 0; i < notifyArray.size(); i++) {
            JSONObject jsonObject = notifyArray.getJSONObject(i);
            int style = jsonObject.getIntValue("style");
            MonitorModel.NotifyType notifyType = BaseEnum.getEnum(MonitorModel.NotifyType.class, style);
            Objects.requireNonNull(notifyType);
            //
            String value = jsonObject.getString("value");
//            if (style == MonitorModel.NotifyType.sms.getCode()) {
//                boolean mobile = Validator.isMobile(value);
//                if (!mobile) {
//                    return JsonMessage.getString(400, "请输入正确的手机号码");
//                }
//            } else
            switch (notifyType) {
                case mail:
                    boolean email = Validator.isEmail(value);
                    if (!email) {
                        return JsonMessage.getString(400, "请输入正确的邮箱");
                    }
                    break;
                case dingding:
                    break;
                default:
                    break;
            }
            notifies.add(new MonitorModel.Notify(style, value));
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
        monitorModel.setNotify(notifies);
        monitorModel.setName(name);
        monitorModel.setModifyTime(DateUtil.date().getTime());
        if (StrUtil.isEmpty(id)) {
            //添加监控
            id = IdUtil.objectId();
            UserModel user = getUser();
            monitorModel.setId(id);
            monitorModel.setParent(user.getName());
            monitorService.addItem(monitorModel);
            return JsonMessage.getString(200, "添加成功");
        }
//        monitorService.openMonitor(monitorModel);
        monitorService.updateItem(monitorModel);
        return JsonMessage.getString(200, "修改成功");
    }

    /**
     * 开启或关闭监控
     */
    @RequestMapping(value = "changeStatus", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String changeStatus(String id, String status) {

        return JsonMessage.getString(200, "");
    }


}
