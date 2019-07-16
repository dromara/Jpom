package cn.keepbx.jpom.service.build;

import cn.hutool.core.date.DateUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.keepbx.jpom.common.BaseOperService;
import cn.keepbx.jpom.model.data.BuildModel;
import cn.keepbx.jpom.system.ServerConfigBean;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * 构建service
 *
 * @author bwcx_jzy
 * @date 2019/7/16
 **/
@Service
public class BuildService extends BaseOperService<BuildModel> {

    @Override
    public List<BuildModel> list() throws IOException {
        JSONObject jsonObject = getJSONObject(ServerConfigBean.BUILD);
        if (jsonObject == null) {
            return null;
        }
        JSONArray jsonArray = formatToArray(jsonObject);
        return jsonArray.toJavaList(BuildModel.class);
    }

    @Override
    public BuildModel getItem(String id) throws IOException {
        return getJsonObjectById(ServerConfigBean.BUILD, id, BuildModel.class);
    }

    @Override
    public void addItem(BuildModel buildModel) {
        saveJson(ServerConfigBean.BUILD, buildModel.toJson());
    }

    @Override
    public void deleteItem(String id) {
        deleteJson(ServerConfigBean.BUILD, id);
    }

    @Override
    public boolean updateItem(BuildModel buildModel) {
        buildModel.setModifyTime(DateUtil.now());
        try {
            updateJson(ServerConfigBean.BUILD, buildModel.toJson());
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
            return false;
        }
        return true;
    }
}
