package cn.keepbx.jpom.service.node;

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
        // 不保存临时数据
        JSONObject jsonObject = outGivingModel.toJson();
        jsonObject.remove("tempCacheMap");
        OutGivingModel newData = jsonObject.toJavaObject(OutGivingModel.class);
        newData.setTempCacheMap(null);
        // 保存
        saveJson(ServerConfigBean.OUTGIVING, newData.toJson());
    }

    @Override
    public boolean updateItem(OutGivingModel outGivingModel) {
        // 不保存临时数据
        JSONObject jsonObject = outGivingModel.toJson();
        jsonObject.remove("tempCacheMap");
        OutGivingModel newData = jsonObject.toJavaObject(OutGivingModel.class);
        newData.setTempCacheMap(null);
        updateJson(ServerConfigBean.OUTGIVING, newData.toJson());
        return true;
    }

    @Override
    public void deleteItem(String id) {
        deleteJson(ServerConfigBean.OUTGIVING, id);
    }
}
