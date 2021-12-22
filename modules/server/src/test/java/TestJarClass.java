/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
import cn.hutool.Hutool;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.URLUtil;
import io.jpom.common.JpomManifest;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Created by jiangzeyin on 2019/4/13.
 */
public class TestJarClass {


	@Test
	public void test(){
		JarFile jarFile = URLUtil.getJarFile(Hutool.class.getResource(""));
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
