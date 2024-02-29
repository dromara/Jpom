/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import org.dromara.jpom.common.JpomManifest;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Created by bwcx_jzy on 2019/4/13.
 */
public class TestJarClass {


    @Test
    public void test() {
        JarFile jarFile = URLUtil.getJarFile(StrUtil.class.getResource(""));
        System.out.println(jarFile.getName());
        URL location = ClassUtil.getLocation(JpomManifest.class);
        System.out.println(location);

        String location1 = ClassUtil.getLocationPath(JpomManifest.class);
        System.out.println(location1);
    }


    public static void main(String[] args) throws IOException {
//        URLClassLoader jarClassLoader = JarClassLoader.loadJarToSystemClassLoader(new File("D:/dev/sdfasdf"));
//        try {
//            Class<?> cls = jarClassLoader.loadClass("cn.keepbx.SpringBootApp");
//            System.out.println(cls);
//        } catch (ClassNotFoundException e) {
//            e.printStackTrace();
//        }
//        Class cls = ClassUtil.loadClass("cn.keepbx.SpringBootApp");
//        System.out.println(cls);
        JarFile jarFile = new JarFile("D:\\SystemDocument\\Desktop\\springboot-test-jar-0.0.1-SNAPSHOT.jar");

        Manifest manifest = jarFile.getManifest();
        Attributes attributes = manifest.getMainAttributes();
        String mainClass = attributes.getValue("Main-Class");
        System.out.println(mainClass);
//        JarClassLoader jarClassLoader = JarClassLoader.load(new File());
//        Enumeration<URL> manifestResources = null;
//        try {
//            manifestResources = jarClassLoader.getResources("META-INF/MANIFEST.MF");
//        } catch (IOException ignored) {
//        }
//        if (manifestResources == null) {
//            return "没有找到对应的MainClass:" + projectInfoModel.getMainClass();
//        }
//        boolean ok = false;
//        while (manifestResources.hasMoreElements()) {
//            try (InputStream inputStream = manifestResources.nextElement().openStream()) {
//
//            } catch (Exception ignored) {
//            }
//        }
    }
}
