package cn.keepbx.jpom.model.data;

import cn.keepbx.jpom.model.BaseModel;
import cn.keepbx.plugin.ClassFeature;
import cn.keepbx.plugin.MethodFeature;

import java.util.List;

/**
 * 角色
 *
 * @author bwcx_jzy
 * @date 2019/8/15
 */
public class RoleModel extends BaseModel {

    private List<RoleFeature> features;

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
