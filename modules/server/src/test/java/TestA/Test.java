package TestA;

import cn.hutool.core.util.ClassUtil;
import cn.keepbx.jpom.service.user.RoleService;
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
        CacheControllerFeature.init();
        Map<ClassFeature, Set<MethodFeature>> classFeatureSetMap = CacheControllerFeature.getFeatureMap();
        ClassFeature monitor = ClassFeature.valueOf("MONITOR");
        System.out.println(monitor.getName());
        System.out.println(classFeatureSetMap);

        Class<?> typeArgument = ClassUtil.getTypeArgument(RoleService.class);
        System.out.println(typeArgument);
    }
}
