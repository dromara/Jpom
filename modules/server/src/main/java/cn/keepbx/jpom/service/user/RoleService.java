package cn.keepbx.jpom.service.user;

import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.jpom.common.BaseOperService;
import cn.keepbx.jpom.model.data.RoleModel;
import cn.keepbx.jpom.service.BaseDynamicService;
import cn.keepbx.jpom.system.ServerConfigBean;
import cn.keepbx.permission.DynamicData;
import cn.keepbx.plugin.ClassFeature;
import com.alibaba.fastjson.JSONArray;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author bwcx_jzy
 * @date 2019/8/15
 */
@Service
public class RoleService extends BaseOperService<RoleModel> {

    public RoleService() {
        super(ServerConfigBean.ROLE);
    }

    /**
     * 获取角色动态数据
     *
     * @param roleId       角色id
     * @param classFeature 功能
     * @return list
     */
    public List<String> listDynamicData(String roleId, ClassFeature classFeature) {
        RoleModel item = getItem(roleId);
        List<String> list = null;
        Map<ClassFeature, List<String>> dynamicData1 = item.getDynamicData();
        if (dynamicData1 != null) {
            list = dynamicData1.get(classFeature);
        }
        return list;
    }

    /**
     * 调用对应功能动态数据list
     *
     * @param roleId
     * @param classFeature
     * @param dataId       数据id
     * @return
     */
    public JSONArray listDynamic(String roleId, ClassFeature classFeature, String dataId) {
        Map<ClassFeature, DynamicData> dynamicDataMap = DynamicData.getDynamicDataMap();
        DynamicData dynamicData = dynamicDataMap.get(classFeature);
        Objects.requireNonNull(dynamicData, "没有配置对应动态数据");
        Class<? extends BaseDynamicService> baseOperService = dynamicData.getBaseOperService();
        BaseDynamicService bean = SpringUtil.getBean(baseOperService);
        return bean.listDynamic(classFeature, roleId, dataId);
    }
}
