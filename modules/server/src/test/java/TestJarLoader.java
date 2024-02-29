/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * @author bwcx_jzy
 * @since 2019/7/23
 **/
public class TestJarLoader {
    public static void main(String[] args) throws ClassNotFoundException, IOException {
        JarFile jarFile1 = new JarFile(new File("D:\\Idea\\Jpom\\modules\\agent\\target\\agent-2.4.2.jar"));
        ZipEntry zipEntry = jarFile1.getEntry("BOOT-INF/classes/cn/keepbx/jpom/common/interceptor/NotAuthorize.class");
        System.out.println(zipEntry);
        String path = "D:\\Idea\\Jpom\\modules\\agent\\target\\agent-2.4.2.jar";//外部jar包的路径
        Set<Class<?>> classes = new LinkedHashSet<>();//所有的Class对象
        Map<Class<?>, Annotation[]> classAnnotationMap = new HashMap<>();//每个Class对象上的注释对象
        Map<Class<?>, Map<Method, Annotation[]>> classMethodAnnoMap = new HashMap<>();//每个Class对象中每个方法上的注释对象
        try {
            JarFile jarFile = new JarFile(new File(path));
            URL url = new URL("file:" + path);
            ClassLoader loader = new URLClassLoader(new URL[]{url});//自己定义的classLoader类，把外部路径也加到load路径里，使系统去该路经load对象
            Enumeration<JarEntry> es = jarFile.entries();
            while (es.hasMoreElements()) {
                JarEntry jarEntry = (JarEntry) es.nextElement();
                String name = jarEntry.getName();
                if (name != null && name.endsWith(".class")) {
                    System.out.println(name);
                    //只解析了.class文件，没有解析里面的jar包
                    //默认去系统已经定义的路径查找对象，针对外部jar包不能用
                    //Class<?> c = Thread.currentThread().getContextClassLoader().loadClass(name.replace("/", ".").substring(0,name.length() - 6));
                    Class<?> c = loader.loadClass(name.replace("/", ".").substring(0, name.length() - 6));//自己定义的loader路径可以找到
                    System.out.println(c);
                    classes.add(c);
                    Annotation[] classAnnos = c.getDeclaredAnnotations();
                    classAnnotationMap.put(c, classAnnos);
                    Method[] classMethods = c.getDeclaredMethods();
                    Map<Method, Annotation[]> methodAnnoMap = new HashMap<>();
                    for (Method classMethod : classMethods) {
                        Annotation[] a = classMethod.getDeclaredAnnotations();
                        methodAnnoMap.put(classMethod, a);
                    }
                    classMethodAnnoMap.put(c, methodAnnoMap);
                }
            }
            System.out.println(classes.size());
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
