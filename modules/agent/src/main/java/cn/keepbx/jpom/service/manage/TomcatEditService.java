package cn.keepbx.jpom.service.manage;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseOperService;
import cn.keepbx.jpom.model.data.TomcatInfoModel;
import cn.keepbx.jpom.system.AgentConfigBean;
import cn.keepbx.jpom.system.JpomRuntimeException;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author lf
 */
@Service
public class TomcatEditService extends BaseOperService<TomcatInfoModel> {

    /**
     * 查询tomcat列表
     *
     * @return Tomcat列表
     */
    @Override
    public List<TomcatInfoModel> list() {
        JSONObject jsonObject = getJSONObject(AgentConfigBean.TOMCAT);
        JSONArray jsonArray = formatToArray(jsonObject);
        return jsonArray.toJavaList(TomcatInfoModel.class);
    }

    /**
     * 查询Tomcat信息
     *
     * @param id 数据id
     * @return Tomcat信息
     */
    @Override
    public TomcatInfoModel getItem(String id) {
        return getJsonObjectById(AgentConfigBean.TOMCAT, id, TomcatInfoModel.class);
    }

    /**
     * 根据tomcat名称查询tomcat信息
     *
     * @param name tomcat的名称
     * @return tomcat信息
     */
    public TomcatInfoModel getItemByName(String name) {
        JSONObject allTomcat = getJSONObject(AgentConfigBean.TOMCAT);

        if (allTomcat == null) {
            return null;
        }

        JSONObject tomcat = null;
        for (String key : allTomcat.keySet()) {
            JSONObject object = allTomcat.getJSONObject(key);
            if (name.equals(object.getString("name"))) {
                tomcat = object;
                break;
            }
        }

        return JSONObject.toJavaObject(tomcat, TomcatInfoModel.class);
    }

    /**
     * 添加Tomcat
     *
     * @param tomcatInfoModel tomcat信息
     */
    @Override
    public void addItem(TomcatInfoModel tomcatInfoModel) {
        tomcatInfoModel.setCreateTime(DateUtil.now());
        saveJson(AgentConfigBean.TOMCAT, tomcatInfoModel.toJson());
    }

    /**
     * 删除tomcat信息
     *
     * @param id 数据id
     */
    @Override
    public void deleteItem(String id) {
        deleteJson(AgentConfigBean.TOMCAT, id);
    }

    /**
     * 修改tomcat信息
     *
     * @param tomcatInfoModel tomcat信息
     * @return 更新结果
     */
    @Override
    public boolean updateItem(TomcatInfoModel tomcatInfoModel) {
        tomcatInfoModel.setModifyTime(DateUtil.now());
        updateJson(AgentConfigBean.TOMCAT, tomcatInfoModel.toJson());
        return true;
    }
}
