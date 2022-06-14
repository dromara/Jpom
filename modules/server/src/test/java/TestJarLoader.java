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
        Set<Class<?>> classes = new LinkedHashSet<Class<?>>();//所有的Class对象
        Map<Class<?>, Annotation[]> classAnnotationMap = new HashMap<Class<?>, Annotation[]>();//每个Class对象上的注释对象
        Map<Class<?>, Map<Method, Annotation[]>> classMethodAnnoMap = new HashMap<Class<?>, Map<Method, Annotation[]>>();//每个Class对象中每个方法上的注释对象
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
                    Map<Method, Annotation[]> methodAnnoMap = new HashMap<Method, Annotation[]>();
                    for (int i = 0; i < classMethods.length; i++) {
                        Annotation[] a = classMethods[i].getDeclaredAnnotations();
                        methodAnnoMap.put(classMethods[i], a);
                    }
                    classMethodAnnoMap.put(c, methodAnnoMap);
                }
            }
            System.out.println(classes.size());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
