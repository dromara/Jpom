package io.jpom.service.monitor;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import io.jpom.common.BaseOperService;
import io.jpom.model.data.MonitorUserOptModel;
import io.jpom.model.log.UserOperateLogV1;
import io.jpom.system.ServerConfigBean;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 监控用户操作Service
 *
 * @author bwcx_jzy
 */
@Service
public class MonitorUserOptService extends BaseOperService<MonitorUserOptModel> {

    public MonitorUserOptService() {
        super(ServerConfigBean.MONITOR_USER_OPT_FILE);
    }

    @Override
    public void updateItem(MonitorUserOptModel monitorUserOptModel) {
        monitorUserOptModel.setModifyTime(DateUtil.date().getTime());
        super.updateItem(monitorUserOptModel);
    }

    public List<MonitorUserOptModel> listByType(UserOperateLogV1.OptType optType) {
        List<MonitorUserOptModel> list = super.list();
        if (CollUtil.isEmpty(list)) {
            return null;
        }
        return list.stream().filter(monitorUserOptModel -> {
            boolean status = monitorUserOptModel.isStatus();
            if (!status) {
                return false;
            }
            List<UserOperateLogV1.OptType> monitorOpt = monitorUserOptModel.getMonitorOpt();

            return CollUtil.contains(monitorOpt, optType);
        }).collect(Collectors.toList());
    }

    public List<MonitorUserOptModel> listByType(UserOperateLogV1.OptType optType, String userId) {
        List<MonitorUserOptModel> userOptModels = this.listByType(optType);
        if (CollUtil.isEmpty(userOptModels)) {
            return null;
        }
        return userOptModels.stream().filter(monitorUserOptModel -> {
            List<String> monitorUser = monitorUserOptModel.getMonitorUser();
            return CollUtil.contains(monitorUser, userId);
        }).collect(Collectors.toList());
    }
}
