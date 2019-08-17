package cn.keepbx.jpom.service.node.ssh;

import cn.hutool.core.util.IdUtil;
import cn.keepbx.jpom.common.BaseOperService;
import cn.keepbx.jpom.model.data.SshModel;
import cn.keepbx.jpom.system.ServerConfigBean;
import cn.keepbx.permission.BaseDynamicService;
import cn.keepbx.plugin.ClassFeature;
import com.alibaba.fastjson.JSONArray;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author bwcx_jzy
 * @date 2019/8/9
 */
@Service
public class SshService extends BaseOperService<SshModel> implements BaseDynamicService {

    public SshService() {
        super(ServerConfigBean.SSH_LIST);
    }

    @Override
    public void addItem(SshModel sshModel) {
        sshModel.setId(IdUtil.fastSimpleUUID());
        super.addItem(sshModel);
    }

    @Override
    public JSONArray listToArray(String dataId) {
        return (JSONArray) JSONArray.toJSON(this.list());
    }

    @Override
    public List<SshModel> list() {
        return (List<SshModel>) filter(super.list(), ClassFeature.SSH);
    }
}
