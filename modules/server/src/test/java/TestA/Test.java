package TestA;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ClassUtil;
import io.jpom.service.user.RoleService;
import io.jpom.permission.CacheControllerFeature;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.MethodFeature;

import java.util.Map;
import java.util.Set;

/**
 * @author bwcx_jzy
 * @date 2019/8/14
 */
public class Test {
    public static void main(String[] args) {

        System.out.println(FileUtil.normalize("sss/ss/../ssss"));
        System.out.println(FileUtil.normalize("./ssss/ssss"));
        CacheControllerFeature.init();
        Map<ClassFeature, Set<MethodFeature>> classFeatureSetMap = CacheControllerFeature.getFeatureMap();
        ClassFeature monitor = ClassFeature.valueOf("MONITOR");
        System.out.println(monitor.getName());
        System.out.println(classFeatureSetMap);

        Class<?> typeArgument = ClassUtil.getTypeArgument(RoleService.class);
        System.out.println(typeArgument);
    }
}
