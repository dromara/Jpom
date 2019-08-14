package cn.keepbx.jpom.service.node.ssh;

import cn.hutool.core.util.IdUtil;
import cn.keepbx.jpom.common.BaseOperService;
import cn.keepbx.jpom.model.data.SshModel;
import cn.keepbx.jpom.system.ServerConfigBean;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * @author bwcx_jzy
 * @date 2019/8/9
 */
@Service
public class SshService extends BaseOperService<SshModel> {

    @Override
    public List<SshModel> list() {
        JSONObject jsonObject = getJSONObject(ServerConfigBean.SSH_LIST);
        JSONArray jsonArray = formatToArray(jsonObject);
        return jsonArray.toJavaList(SshModel.class);
    }

    @Override
    public SshModel getItem(String id) throws IOException {
        return getJsonObjectById(ServerConfigBean.SSH_LIST, id, SshModel.class);
    }

    @Override
    public void addItem(SshModel sshModel) {
        sshModel.setId(IdUtil.fastSimpleUUID());
        saveJson(ServerConfigBean.SSH_LIST, sshModel.toJson());
    }

    @Override
    public void deleteItem(String id) {
        deleteJson(ServerConfigBean.SSH_LIST, id);
    }

    @Override
    public boolean updateItem(SshModel sshModel) throws Exception {
        updateJson(ServerConfigBean.SSH_LIST, sshModel.toJson());
        return true;
    }
}
