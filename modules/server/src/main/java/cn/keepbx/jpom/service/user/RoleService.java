package cn.keepbx.jpom.service.user;

import cn.hutool.core.date.DateTime;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.jpom.common.BaseOperService;
import cn.keepbx.jpom.model.data.RoleModel;
import cn.keepbx.jpom.model.data.UserModel;
import cn.keepbx.jpom.system.ServerConfigBean;
import cn.keepbx.permission.BaseDynamicService;
import cn.keepbx.permission.DynamicData;
import cn.keepbx.plugin.ClassFeature;
import cn.keepbx.plugin.MethodFeature;
import com.alibaba.fastjson.JSONArray;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @author bwcx_jzy
 * @date 2019/8/15
 */
@Service
public class RoleService extends BaseOperService<RoleModel> {

    public RoleService() {
        super(ServerConfigBean.ROLE);
    }

    @Override
    public void addItem(RoleModel roleModel) {
        roleModel.setUpdateTime(DateTime.now().toString());
        super.addItem(roleModel);
    }

    @Override
    public void updateItem(RoleModel roleModel) {
        roleModel.setUpdateTime(DateTime.now().toString());
        super.updateItem(roleModel);
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


    public boolean errorDynamicPermission(UserModel userModel, ClassFeature classFeature, String dataId) {
        DynamicData dynamicData1 = DynamicData.getDynamicDataMap().get(classFeature);
        if (dynamicData1 == null) {
            // 如果不是没有动态权限  就默认通过
            return false;
        }
        if (userModel.isSystemUser()) {
            return false;
        }
        Set<String> roles = userModel.getRoles();
        if (roles == null || roles.isEmpty()) {
            return true;
        }
        for (String role : roles) {
            RoleModel item = getItem(role);
            if (item == null) {
                continue;
            }
            Map<ClassFeature, List<String>> dynamicData = item.getDynamicData();
            if (dynamicData == null) {
                continue;
            }
            List<String> list = dynamicData.get(classFeature);
            if (list.contains(dataId)) {
                return false;
            }
        }
        return true;
    }

    public boolean errorMethodPermission(UserModel userModel, ClassFeature classFeature, MethodFeature methodFeature) {
        if (userModel.isSystemUser()) {
            return false;
        }
        Set<String> roles = userModel.getRoles();
        if (roles == null || roles.isEmpty()) {
            return true;
        }
        for (String role : roles) {
            RoleModel item = getItem(role);
            if (item == null) {
                continue;
            }
            List<MethodFeature> methodFeatures = item.getMethodFeature(classFeature);
            if (methodFeatures == null) {
                continue;
            }
            if (methodFeatures.contains(methodFeature)) {
                return false;
            }
        }
        return true;
    }

    public Set<String> getDynamicList(UserModel userModel, ClassFeature classFeature) {
        Set<String> roles = userModel.getRoles();
        if (roles == null || roles.isEmpty()) {
            return null;
        }
        Set<String> allData = new HashSet<>();
        for (String role : roles) {
            RoleModel item = getItem(role);
            if (item == null) {
                continue;
            }
            Map<ClassFeature, List<String>> dynamicData = item.getDynamicData();
            if (dynamicData == null) {
                continue;
            }
            List<String> list = dynamicData.get(classFeature);
            if (list != null) {
                allData.addAll(list);
            }
        }
        return allData;
    }
}
