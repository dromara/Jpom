package io.jpom.system.init;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.PreLoadClass;
import cn.jiangzeyin.common.PreLoadMethod;
import cn.jiangzeyin.common.spring.SpringUtil;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.JpomManifest;
import io.jpom.common.Type;
import io.jpom.model.data.NodeModel;
import io.jpom.model.system.AgentAutoUser;
import io.jpom.service.node.NodeService;
import io.jpom.system.ConfigBean;
import io.jpom.system.ServerConfigBean;
import io.jpom.util.JsonFileUtil;
import io.jpom.util.JvmUtil;
import sun.jvmstat.monitor.MonitoredVm;
import sun.jvmstat.monitor.VmIdentifier;

import java.io.File;
import java.util.List;

/**
 * 自动导入本机节点
 *
 * @author jiangzeyin
 * @date 2019/4/18
 */
@PreLoadClass
public class AutoImportLocalNode {

    private static final String AGENT_MAIN_CLASS = "io.jpom.JpomAgentApplication";
    private static NodeService nodeService;

    @PreLoadMethod
    private static void install() {
        File file = FileUtil.file(ConfigBean.getInstance().getDataPath(), ServerConfigBean.INSTALL);
        if (file.exists()) {
            return;
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("installId", IdUtil.fastSimpleUUID());
        jsonObject.put("installTime", DateTime.now().toString());
        jsonObject.put("desc", "请勿删除此文件,服务端安装id和插件端互通关联");
        JsonFileUtil.saveJson(file.getAbsolutePath(), jsonObject);
    }

    @PreLoadMethod
    private static void loadAgent() {
        nodeService = SpringUtil.getBean(NodeService.class);
        List<NodeModel> list = nodeService.list();
        if (list != null && !list.isEmpty()) {
            return;
        }
        //
        try {
            List<MonitoredVm> monitoredVms = JvmUtil.listMainClass(AGENT_MAIN_CLASS);
            monitoredVms.forEach(monitoredVm -> {
                VmIdentifier vmIdentifier = monitoredVm.getVmIdentifier();
                findPid(vmIdentifier.getUserInfo());
            });
        } catch (Exception e) {
            DefaultSystemLog.getLog().error("自动添加本机节点错误", e);
        }
    }

    private static void findPid(String pid) {
        File file = ConfigBean.getInstance().getApplicationJpomInfo(Type.Agent);
        if (!file.exists() || file.isDirectory()) {
            return;
        }
        // 比较进程id
        String json = FileUtil.readString(file, CharsetUtil.CHARSET_UTF_8);
        JpomManifest jpomManifest = JSONObject.parseObject(json, JpomManifest.class);
        if (!pid.equals(String.valueOf(jpomManifest.getPid()))) {
            return;
        }
        // 判断自动授权文件是否存在
        String path = ConfigBean.getInstance().getAgentAutoAuthorizeFile(jpomManifest.getDataPath());
        if (!FileUtil.exist(path)) {
            return;
        }
        json = FileUtil.readString(path, CharsetUtil.CHARSET_UTF_8);
        AgentAutoUser autoUser = JSONObject.parseObject(json, AgentAutoUser.class);
        // 判断授权信息
        //
        NodeModel nodeModel = new NodeModel();
        nodeModel.setUrl(StrUtil.format("127.0.0.1:{}", jpomManifest.getPort()));
        nodeModel.setName("本机");
        nodeModel.setId("localhost");
        //
        nodeModel.setLoginPwd(autoUser.getAgentPwd());
        nodeModel.setLoginName(autoUser.getAgentName());
        //
        nodeModel.setOpenStatus(true);
        nodeService.addItem(nodeModel);
        Console.log("自动添加本机节点成功：" + nodeModel.getId());
    }
}
