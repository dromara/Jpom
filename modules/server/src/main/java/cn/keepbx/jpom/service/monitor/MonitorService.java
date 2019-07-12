package cn.keepbx.jpom.service.monitor;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.cron.CronUtil;
import cn.hutool.cron.Scheduler;
import cn.hutool.cron.task.Task;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseOperService;
import cn.keepbx.jpom.common.forward.NodeForward;
import cn.keepbx.jpom.common.forward.NodeUrl;
import cn.keepbx.jpom.model.BaseEnum;
import cn.keepbx.jpom.model.data.MonitorModel;
import cn.keepbx.jpom.model.data.NodeModel;
import cn.keepbx.jpom.service.node.NodeService;
import cn.keepbx.jpom.system.ConfigBean;
import cn.keepbx.jpom.system.JpomRuntimeException;
import cn.keepbx.jpom.system.ServerConfigBean;
import cn.keepbx.jpom.util.DingTalkUtil;
import cn.keepbx.jpom.util.EmailUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * 监控管理Service
 *
 * @author Arno
 */
@Service
public class MonitorService extends BaseOperService<MonitorModel> {

    @Resource
    private NodeService nodeService;

    @Override
    public List<MonitorModel> list() {
        JSONObject jsonObject = getJSONObject(ServerConfigBean.MONITOR_FILE);
        if (jsonObject == null) {
            return null;
        }
        Set<Map.Entry<String, Object>> set = jsonObject.entrySet();
        List<MonitorModel> array = new ArrayList<>();
        for (Map.Entry entry : set) {
            JSONObject value = (JSONObject) entry.getValue();
            MonitorModel monitorModel = value.toJavaObject(MonitorModel.class);
            array.add(monitorModel);
        }
        return array;
    }

    @Override
    public MonitorModel getItem(String id) {
        return getJsonObjectById(ServerConfigBean.MONITOR_FILE, id, MonitorModel.class);
    }

    @Override
    public void addItem(MonitorModel monitorModel) {
        saveJson(ServerConfigBean.MONITOR_FILE, monitorModel.toJson());
    }

    @Override
    public void deleteItem(String id) {
        deleteJson(ServerConfigBean.MONITOR_FILE, id);
    }

    @Override
    public boolean updateItem(MonitorModel monitorModel) {
        try {
            updateJson(ServerConfigBean.MONITOR_FILE, monitorModel.toJson());
            return true;
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
        }
        return false;
    }

    /**
     * 开启监控
     *
     * @param monitorModel 监控信息
     */
    public void openMonitor(MonitorModel monitorModel) {
        String schedulerId = monitorModel.getId();
        CronUtil.remove(schedulerId);
        CronUtil.setMatchSecond(true);
        String pattern = String.format("%d * * * * ?", monitorModel.getCycle());
        //添加定时任务
        CronUtil.schedule(schedulerId, "0/5 * * * * ?", () -> {
            JSONArray projects = monitorModel.getProjects();
            for (int i = 0; i < projects.size(); i++) {
                JSONObject item = projects.getJSONObject(i);
                String nodeId = item.getString("node");
                NodeModel nodeModel = nodeService.getItem(nodeId);
                JSONArray projectsArray = item.getJSONArray("projects");
                for (Object o : projectsArray) {
                    String project = (String) o;
                    Map<String, Object> map = new HashMap<>(1);
                    map.put("id", project);
                    //查询项目运行状态
                    String url = nodeModel.getRealUrl(NodeUrl.Manage_GetProjectStatus);
                    HttpRequest post = HttpUtil.createPost(url).form(map);
                    post.header(ConfigBean.JPOM_AGENT_AUTHORIZE, nodeModel.getAuthorize(true));
                    String body = post.execute().body();
                    JSONObject result = JSONObject.parseObject(body);
                    //处理项目状态查询后的结果
                    handle(monitorModel, result);
                }
            }
        });
        Scheduler scheduler = CronUtil.getScheduler();
        if (!scheduler.isStarted()) {
            CronUtil.start();
        }
    }

    /**
     * 判断监控是否运行
     *
     * @param id 监控id
     * @return true -运行，false-停止
     */
    public boolean checkMonitorRunning(String id) {
        Scheduler scheduler = CronUtil.getScheduler();
        Task task = scheduler.getTask(id);
        return task != null;
    }

    /**
     * 处理项目状态查询后的结果
     *
     * @param monitorModel 监控信息
     * @param result       结果
     */
    private void handle(MonitorModel monitorModel, JSONObject result) {
        int code = result.getIntValue("code");
        if (code != 200) {
            String msg = result.getString("msg");
            DefaultSystemLog.ERROR().error("查询项目状态异常：" + msg);
            throw new JpomRuntimeException("查询项目状态异常：" + msg);
        }
        JSONObject data = result.getJSONObject("data");
        int pId = data.getIntValue("pId");
        if (pId != 0) {
            return;
        }
        JSONArray notify = monitorModel.getNotify();
        for (int i = 0; i < notify.size(); i++) {
            JSONObject jsonObject = notify.getJSONObject(i);
            int style = jsonObject.getIntValue("style");
            String value = jsonObject.getString("value");
            BaseEnum anEnum = BaseEnum.getEnum(MonitorModel.NotifyType.class, style);
            if (anEnum == MonitorModel.NotifyType.dingding) {
//                DingTalkUtil.sendMsg(value, "", "");
            } else if (anEnum == MonitorModel.NotifyType.sms) {

            } else if (anEnum == MonitorModel.NotifyType.mail) {
//                EmailUtil.sendHtmlToEmail("Jpom监控警报", value, "Jpom项目监控警报", "");
            }
        }
    }

    /**
     * 关闭监控
     *
     * @param id 监控id
     */
    public void stopMonitor(String id) {
        CronUtil.remove(id);
    }

}
