package cn.keepbx.jpom.service;

import cn.keepbx.jpom.common.BaseOperService;
import cn.keepbx.jpom.model.BaseModel;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.List;

/**
 * @author bwcx_jzy
 * @date 2019/8/15
 */
public abstract class BaseDynamicService<T extends BaseModel> extends BaseOperService<T> {

    public BaseDynamicService(String fileName) {
        super(fileName);
    }

    public JSONArray listDynamic() {
        List<T> list = super.list();
        if (list == null || list.isEmpty()) {
            return null;
        }
        JSONArray jsonArray = new JSONArray();
        list.forEach(baseModel -> {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("title", baseModel.getName());
            jsonObject.put("id", baseModel.getId());
            jsonArray.add(jsonObject);
        });
        return jsonArray;
    }
}
