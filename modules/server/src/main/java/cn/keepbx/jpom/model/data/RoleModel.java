package cn.keepbx.jpom.model.data;

import cn.keepbx.jpom.model.BaseModel;
import cn.keepbx.plugin.ClassFeature;
import cn.keepbx.plugin.MethodFeature;

import java.util.List;
import java.util.Map;

/**
 * 角色
 *
 * @author bwcx_jzy
 * @date 2019/8/15
 */
public class RoleModel extends BaseModel {

    /**
     * 角色使用功能
     */
    private List<RoleFeature> features;

    /**
     * 动态数据
     */
    private Map<ClassFeature, List<String>> dynamicData;

    /**
     * 修改时间
     */
    private String updateTime;

    private int bindCount;

    public int getBindCount() {
        return bindCount;
    }

    public void setBindCount(int bindCount) {
        this.bindCount = bindCount;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public Map<ClassFeature, List<String>> getDynamicData() {
        return dynamicData;
    }

    public void setDynamicData(Map<ClassFeature, List<String>> dynamicData) {
        this.dynamicData = dynamicData;
    }

    public List<RoleFeature> getFeatures() {
        return features;
    }

    public void setFeatures(List<RoleFeature> features) {
        this.features = features;
    }

    public List<MethodFeature> getMethodFeature(ClassFeature feature) {
        List<RoleFeature> features = getFeatures();
        for (RoleFeature roleFeature : features) {
            if (roleFeature.getFeature() == feature) {
                return roleFeature.getMethodFeatures();
            }
        }
        return null;
    }

    public static class RoleFeature {
        private ClassFeature feature;
        private List<MethodFeature> methodFeatures;

        public ClassFeature getFeature() {
            return feature;
        }

        public void setFeature(ClassFeature feature) {
            this.feature = feature;
        }

        public List<MethodFeature> getMethodFeatures() {
            return methodFeatures;
        }

        public void setMethodFeatures(List<MethodFeature> methodFeatures) {
            this.methodFeatures = methodFeatures;
        }
    }
}
