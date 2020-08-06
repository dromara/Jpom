package io.jpom.service.monitor;

import io.jpom.common.BaseOperService;
import io.jpom.model.data.MonitorUserOptModel;
import io.jpom.system.ServerConfigBean;
import org.springframework.stereotype.Service;

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
}
