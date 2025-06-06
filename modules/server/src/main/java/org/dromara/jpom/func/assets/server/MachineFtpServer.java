package org.dromara.jpom.func.assets.server;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.cron.task.Task;
import cn.hutool.db.Entity;
import cn.hutool.extra.ftp.Ftp;
import cn.hutool.extra.ftp.FtpConfig;
import cn.hutool.extra.ftp.FtpMode;
import cn.keepbx.jpom.event.IAsyncLoad;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.Const;
import org.dromara.jpom.common.ServerConst;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.configuration.AssetsConfig;
import org.dromara.jpom.cron.CronUtils;
import org.dromara.jpom.func.assets.AssetsExecutorPoolService;
import org.dromara.jpom.func.assets.model.MachineFtpModel;
import org.dromara.jpom.func.system.service.ClusterInfoService;
import org.dromara.jpom.plugin.IWorkspaceEnvPlugin;
import org.dromara.jpom.plugin.PluginFactory;
import org.dromara.jpom.service.h2db.BaseDbService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author bwcx_jzy
 * @since 2024/8/31
 */
@Service
@Slf4j
public class MachineFtpServer extends BaseDbService<MachineFtpModel> implements IAsyncLoad, Task {

    private static final String CRON_ID = "ftp-monitor";

    private final ClusterInfoService clusterInfoService;
    private final AssetsConfig.FtpConfig ftpConfig;
    private final AssetsExecutorPoolService assetsExecutorPoolService;

    public MachineFtpServer(ClusterInfoService clusterInfoService,
                            AssetsConfig assetsConfig,
                            AssetsExecutorPoolService assetsExecutorPoolService) {
        this.clusterInfoService = clusterInfoService;
        this.ftpConfig = assetsConfig.getFtp();
        this.assetsExecutorPoolService = assetsExecutorPoolService;
    }

    /**
     * 转换为配置对象
     *
     * @param model ftp model
     * @return config
     */
    public FtpConfig toFtpConfig(MachineFtpModel model) {
        String workspaceId = ServerConst.WORKSPACE_GLOBAL;
        FtpConfig config = model.toFtpConfig();
        String password = config.getPassword();
        String user = config.getUser();
        // 转化密码字段
        IWorkspaceEnvPlugin plugin = (IWorkspaceEnvPlugin) PluginFactory.getPlugin(IWorkspaceEnvPlugin.PLUGIN_NAME);
        try {
            user = plugin.convertRefEnvValue(workspaceId, user);
            password = plugin.convertRefEnvValue(workspaceId, password);
        } catch (Exception e) {
            throw Lombok.sneakyThrow(e);
        }
        config.setUser(user);
        config.setPassword(password);
        return config;
    }

    @Override
    protected void fillInsert(MachineFtpModel model) {
        super.fillInsert(model);
        model.setGroupName(StrUtil.emptyToDefault(model.getGroupName(), Const.DEFAULT_GROUP_NAME.get()));
        model.setStatus(ObjectUtil.defaultIfNull(model.getStatus(), 0));
    }

    @Override
    protected void fillSelectResult(MachineFtpModel data) {
        if (data == null) {
            return;
        }
        if (!StrUtil.startWithIgnoreCase(data.getPassword(), ServerConst.REF_WORKSPACE_ENV)) {
            // 隐藏密码字段
            data.setPassword(null);
        }
    }

    @Override
    public void execute() {
        Entity entity = new Entity();
        if (clusterInfoService.isMultiServer()) {
            String linkGroup = clusterInfoService.getCurrent().getLinkGroup();
            List<String> linkGroups = StrUtil.splitTrim(linkGroup, StrUtil.COMMA);
            if (CollUtil.isEmpty(linkGroups)) {
                log.warn("当前集群还未绑定分组,不能监控 FTP 资产信息");
                return;
            }
            entity.set("groupName", linkGroups);
        }
        List<MachineFtpModel> list = this.listByEntity(entity, false);
        if (CollUtil.isEmpty(list)) {
            return;
        }
        this.checkList(list);
    }


    private void checkList(List<MachineFtpModel> monitorModels) {
        monitorModels.forEach(monitorModel -> assetsExecutorPoolService.execute(() -> this.updateMonitor(monitorModel)));
    }

    /**
     * 执行监控 ftp
     *
     * @param machineFtpModel 资产 ftp
     */
    private void updateMonitor(MachineFtpModel machineFtpModel) {
        List<String> monitorGroupName = ftpConfig.getDisableMonitorGroupName();
        if (CollUtil.containsAny(monitorGroupName, CollUtil.newArrayList(machineFtpModel.getGroupName(), "*"))) {
            // 禁用监控
            if (machineFtpModel.getStatus() != null && machineFtpModel.getStatus() == 2) {
                // 不需要更新
                return;
            }
            this.updateStatus(machineFtpModel.getId(), 2, I18nMessageUtil.get("i18n.disable_monitoring.4615"));
            return;
        }

        try (Ftp ftp = new Ftp(this.toFtpConfig(machineFtpModel),
            EnumUtil.fromString(FtpMode.class, machineFtpModel.getMode(), FtpMode.Active))) {
            ftp.pwd();
            //
            this.updateStatus(machineFtpModel.getId(), 1, "");
        } catch (Exception e) {
            String message = e.getMessage();
            String s = "监控";
            if (StrUtil.containsIgnoreCase(message, "timeout")) {
                String s1 = "超时";
                log.error("{} ftp[{}] {} {}", s, machineFtpModel.getName(), s1, message);
            } else {
                String s1 = "异常";
                log.error("{} ftp[{}] {}", s, machineFtpModel.getName(), s1, e);
            }
            this.updateStatus(machineFtpModel.getId(), 0, message);
        }
    }

    /**
     * 更新 ftp状态
     *
     * @param id     ID
     * @param status 状态值
     * @param msg    错误消息
     */
    private void updateStatus(String id, int status, String msg) {
        MachineFtpModel model = new MachineFtpModel();
        model.setId(id);
        model.setStatus(status);
        model.setStatusMsg(msg);
        super.updateById(model);
    }

    @Override
    public void startLoad() {
        String monitorCron = ftpConfig.getMonitorCron();
        String cron = Opt.ofBlankAble(monitorCron).orElse("0 0/1 * * * ?");
        CronUtils.add(CRON_ID, cron, () -> MachineFtpServer.this);
    }
}
