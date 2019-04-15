import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Created by jiangzeyin on 2019/4/13.
 */
public class TestJarClass {
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
