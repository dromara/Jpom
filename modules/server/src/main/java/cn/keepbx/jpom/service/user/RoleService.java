package cn.keepbx.jpom.service.user;

import cn.keepbx.jpom.common.BaseOperService;
import cn.keepbx.jpom.model.data.RoleModel;
import cn.keepbx.jpom.system.ServerConfigBean;
import org.springframework.stereotype.Service;

/**
 * @author bwcx_jzy
 * @date 2019/8/15
 */
@Service
public class RoleService extends BaseOperService<RoleModel> {

    public RoleService() {
        super(ServerConfigBean.ROLE);
    }
}
