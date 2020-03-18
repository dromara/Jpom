package io.jpom.service.dblog;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import io.jpom.model.BaseEnum;
import io.jpom.model.data.BuildModel;
import io.jpom.model.data.MonitorModel;
import io.jpom.model.data.MonitorUserOptModel;
import io.jpom.model.data.UserModel;
import io.jpom.model.log.UserOperateLogV1;
import io.jpom.monitor.NotifyUtil;
import io.jpom.service.build.BuildService;
import io.jpom.service.monitor.MonitorUserOptService;
import io.jpom.service.user.UserService;
import io.jpom.system.db.DbConfig;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 操作日志
 *
 * @author bwcx_jzy
 * @date 2019/7/20
 */
@Service
public class DbUserOperateLogService extends BaseDbLogService<UserOperateLogV1> {

    private final MonitorUserOptService monitorUserOptService;
    private final UserService userService;
    private final BuildService buildService;

    public DbUserOperateLogService(MonitorUserOptService monitorUserOptService,
                                   UserService userService,
                                   BuildService buildService) {
        super(UserOperateLogV1.TABLE_NAME, UserOperateLogV1.class);
        this.monitorUserOptService = monitorUserOptService;
        this.userService = userService;
        this.buildService = buildService;
        setKey("reqId");
    }

    @Override
    public void insert(UserOperateLogV1 userOperateLogV1) {
        super.insert(userOperateLogV1);
        DbConfig.autoClear(getTableName(), "optTime");
        ThreadUtil.execute(() -> {
            UserOperateLogV1.OptType optType = BaseEnum.getEnum(UserOperateLogV1.OptType.class, userOperateLogV1.getOptType());
            if (optType == null) {
                return;
            }
            UserModel optUserItem = userService.getItem(userOperateLogV1.getUserId());
            if (optUserItem == null) {
                return;
            }
            String otherMsg = "";
            if (optType == UserOperateLogV1.OptType.StartBuild || optType == UserOperateLogV1.OptType.EditBuild) {
                BuildModel item = buildService.getItem(userOperateLogV1.getDataId());
                if (item != null) {
                    otherMsg = StrUtil.format("操作的构建名称：{}\n", item.getName());
                }
            }
            List<MonitorUserOptModel> monitorUserOptModels = monitorUserOptService.listByType(optType, userOperateLogV1.getUserId());
            if (CollUtil.isEmpty(monitorUserOptModels)) {
                return;
            }
            for (MonitorUserOptModel monitorUserOptModel : monitorUserOptModels) {
                List<String> notifyUser = monitorUserOptModel.getNotifyUser();
                if (CollUtil.isEmpty(notifyUser)) {
                    continue;
                }
                for (String userId : notifyUser) {
                    UserModel item = userService.getItem(userId);
                    if (item == null) {
                        continue;
                    }
                    //
                    String context = StrUtil.format("操作用户：{}\n操作状态：{}\n操作类型：{}\n操作节点：{}\n 操作数据id: {}\n操作IP: {}\n{}",
                            optUserItem.getName(), userOperateLogV1.getOptStatusMsg(), userOperateLogV1.getOptTypeMsg(),
                            userOperateLogV1.getNodeId(), userOperateLogV1.getDataId(), userOperateLogV1.getIp(), otherMsg);
                    // 邮箱
                    String email = item.getEmail();
                    if (StrUtil.isNotEmpty(email)) {
                        MonitorModel.Notify notify1 = new MonitorModel.Notify(MonitorModel.NotifyType.mail, email);
                        ThreadUtil.execute(() -> NotifyUtil.send(notify1, "用户操作报警", context));

                    }
                    // dingding
                    String dingDing = item.getDingDing();
                    if (StrUtil.isNotEmpty(dingDing)) {
                        MonitorModel.Notify notify1 = new MonitorModel.Notify(MonitorModel.NotifyType.dingding, dingDing);
                        ThreadUtil.execute(() -> NotifyUtil.send(notify1, "用户操作报警", context));
                    }
                    // 企业微信
                    String workWx = item.getWorkWx();
                    if (StrUtil.isNotEmpty(workWx)) {
                        MonitorModel.Notify notify1 = new MonitorModel.Notify(MonitorModel.NotifyType.workWx, workWx);
                        ThreadUtil.execute(() -> NotifyUtil.send(notify1, "用户操作报警", context));
                    }
                }
            }
        });
    }
}
