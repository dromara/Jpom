package cn.keepbx.jpom.service.manage;

import cn.hutool.core.date.DateUtil;
import cn.keepbx.jpom.common.BaseOperService;
import cn.keepbx.jpom.model.data.TomcatInfoModel;
import cn.keepbx.jpom.system.AgentConfigBean;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

/**
 * @author lf
 */
@Service
public class TomcatEditService extends BaseOperService<TomcatInfoModel> {


    public TomcatEditService() {
        super(AgentConfigBean.TOMCAT);
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
        super.addItem(tomcatInfoModel);
    }


    /**
     * 修改tomcat信息
     *
     * @param tomcatInfoModel tomcat信息
     * @return 更新结果
     */
    @Override
    public void updateItem(TomcatInfoModel tomcatInfoModel) {
        tomcatInfoModel.setModifyTime(DateUtil.now());
        super.updateItem(tomcatInfoModel);

    }
}
