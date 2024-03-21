/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
//package TestA;
//
//import cn.hutool.core.io.FileUtil;
//import io.jpom.permission.CacheControllerFeature;
//import io.jpom.permission.ClassFeature;
//import io.jpom.permission.MethodFeature;
//
//import java.util.Map;
//import java.util.Set;
//
///**
// * @author bwcx_jzy
// * @since 2019/8/14
// */
//public class Test {
//    public static void main(String[] args) {
//
//        System.out.println(FileUtil.normalize("sss/ss/../ssss"));
//        System.out.println(FileUtil.normalize("./ssss/ssss"));
//        CacheControllerFeature.init();
//        Map<ClassFeature, Set<MethodFeature>> classFeatureSetMap = CacheControllerFeature.getFeatureMap();
//        ClassFeature monitor = ClassFeature.valueOf("MONITOR");
//        System.out.println(monitor.getName());
//        System.out.println(classFeatureSetMap);
//
////        Class<?> typeArgument = ClassUtil.getTypeArgument(RoleService.class);
////        System.out.println(typeArgument);
//    }
//}
