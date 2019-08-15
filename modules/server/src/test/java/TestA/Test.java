package TestA;

import cn.keepbx.permission.CacheControllerFeature;
import cn.keepbx.plugin.ClassFeature;
import cn.keepbx.plugin.MethodFeature;

import java.util.Map;
import java.util.Set;

/**
 * @author bwcx_jzy
 * @date 2019/8/14
 */
public class Test {
    public static void main(String[] args) {
        Map<ClassFeature, Set<MethodFeature>> classFeatureSetMap = CacheControllerFeature.init();
        System.out.println(classFeatureSetMap);
    }
}
