package cn.keepbx.jpom.service.node.ssh;

import cn.hutool.core.util.IdUtil;
import cn.keepbx.jpom.common.BaseOperService;
import cn.keepbx.jpom.model.data.SshModel;
import cn.keepbx.jpom.system.ServerConfigBean;
import org.springframework.stereotype.Service;

/**
 * @author bwcx_jzy
 * @date 2019/8/9
 */
@Service
public class SshService extends BaseOperService<SshModel> {

    public SshService() {
        super(ServerConfigBean.SSH_LIST);
    }

    @Override
    public void addItem(SshModel sshModel) {
        sshModel.setId(IdUtil.fastSimpleUUID());
        super.addItem(sshModel);
    }
}
