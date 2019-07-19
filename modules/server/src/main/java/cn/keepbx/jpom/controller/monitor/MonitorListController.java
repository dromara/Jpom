package cn.keepbx.jpom.controller.monitor;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorConfig;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.common.interceptor.UrlPermission;
import cn.keepbx.jpom.model.BaseEnum;
import cn.keepbx.jpom.model.Role;
import cn.keepbx.jpom.model.data.*;
import cn.keepbx.jpom.service.monitor.MonitorMailConfigService;
import cn.keepbx.jpom.service.monitor.MonitorService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.sql.SQLException;
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

    @Resource
    private MonitorMailConfigService monitorMailConfigService;

    /**
     * 展示监控页面
     */
    @RequestMapping(value = "list.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String list() {
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
    @UrlPermission(value = Role.System, optType = UserOperateLogV1.OptType.DelMonitor)
    public String deleteMonitor(@ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "删除失败")) String id) throws SQLException {
        // 删除日志
        Entity where = new Entity(MonitorNotifyLog.TABLE_NAME);
        where.set("monitorId", id);
        Db db = Db.use();
        db.setWrapper((Character) null);
        db.del(where);
        //
        monitorService.deleteItem(id);
        return JsonMessage.getString(200, "删除成功");
    }

    private String checkNotifyData(String notify, List<MonitorModel.Notify> notifies) {
        JSONArray notifyArray = JSONArray.parseArray(notify);
        if (notify == null || notifyArray.isEmpty()) {
            return JsonMessage.getString(400, "请至少选择一种通知方式");
        }
        for (int i = 0; i < notifyArray.size(); i++) {
            JSONObject jsonObject = notifyArray.getJSONObject(i);
            int style = jsonObject.getIntValue("style");
            MonitorModel.NotifyType notifyType = BaseEnum.getEnum(MonitorModel.NotifyType.class, style);
            Objects.requireNonNull(notifyType);
            //
            String value = jsonObject.getString("value");
            if (StrUtil.isBlank(value)) {
                return JsonMessage.getString(405, "请填写通知信息");
            }
            switch (notifyType) {
                case mail:
                    // 检查配置
                    MailAccountModel config = monitorMailConfigService.getConfig();
                    if (config == null) {
                        return JsonMessage.getString(400, "还没有配置邮箱信息，请选配置邮箱信息");
                    }
                    //
                    String[] emails = StrUtil.split(value, StrUtil.COMMA);
                    if (emails == null || emails.length <= 0) {
                        return JsonMessage.getString(400, "请输入邮箱");
                    }
                    for (String email : emails) {
                        if (!Validator.isEmail(email)) {
                            return JsonMessage.getString(400, "请输入正确的邮箱:" + email);
                        }
                    }
                    break;
                case dingding:
                    if (!Validator.isUrl(value)) {
                        return JsonMessage.getString(400, "钉钉通知地址不正确");
                    }
                    break;
                default:
                    break;
            }
            notifies.add(new MonitorModel.Notify(style, value));
        }
        return null;
    }

    /**
     * 增加或修改监控
     */
    @RequestMapping(value = "updateMonitor", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @UrlPermission(value = Role.ServerManager, optType = UserOperateLogV1.OptType.EditMonitor)
    public String updateMonitor(String id,
                                @ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "监控名称不能为空")) String name,
                                String notify) {
        int cycle = getParameterInt("cycle", MonitorModel.Cycle.five.getCode());
        String status = getParameter("status");
        String autoRestart = getParameter("autoRestart");
        List<MonitorModel.Notify> notifies = new ArrayList<>();
        String error = checkNotifyData(notify, notifies);
        if (error != null) {
            return error;
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
     */
    @RequestMapping(value = "changeStatus", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @UrlPermission(value = Role.ServerManager, optType = UserOperateLogV1.OptType.ChangeStatusMonitor)
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
