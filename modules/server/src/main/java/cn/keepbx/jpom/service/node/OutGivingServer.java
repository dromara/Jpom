package cn.keepbx.jpom.service.node;

import cn.jiangzeyin.common.DefaultSystemLog;
import cn.keepbx.jpom.common.BaseOperService;
import cn.keepbx.jpom.model.data.OutGivingModel;
import cn.keepbx.jpom.system.ServerConfigBean;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * 分发管理
 *
 * @author jiangzeyin
 * @date 2019/4/21
 */
@Service
public class OutGivingServer extends BaseOperService<OutGivingModel> {

    @Override
    public List<OutGivingModel> list() throws IOException {
        JSONObject jsonObject = getJSONObject(ServerConfigBean.OUTGIVING);
        JSONArray jsonArray = formatToArray(jsonObject);
        return jsonArray.toJavaList(OutGivingModel.class);
    }

    @Override
    public OutGivingModel getItem(String id) throws IOException {
        return getJsonObjectById(ServerConfigBean.OUTGIVING, id, OutGivingModel.class);
    }

    @Override
    public void addItem(OutGivingModel outGivingModel) {
        // 保存
        saveJson(ServerConfigBean.OUTGIVING, outGivingModel.toJson());
    }

    @Override
    public boolean updateItem(OutGivingModel outGivingModel) {
        updateJson(ServerConfigBean.OUTGIVING, outGivingModel.toJson());
        return true;
    }

    public boolean deleteItem(String id) {
        try {
            deleteJson(ServerConfigBean.OUTGIVING, id);
            return true;
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
        }
        return false;
    }
}
