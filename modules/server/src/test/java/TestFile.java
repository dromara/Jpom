import cn.hutool.core.io.FileUtil;

import java.io.File;
import java.io.IOException;

/**
 * Created by jiangzeyin on 2019/3/15.
 */
public class TestFile {
    public static void main(String[] args) throws IOException {
//        File file = new File("C:/WINDOWS/system32/s/s");
//        System.out.println(file.toPath().startsWith(new File("C:/Windows/System32/s/S").toPath()));
////        System.out.println(file());
//
//
//        File file1 = new File("D:/keystore.p12");
//        System.out.println(file1.exists() && file1.isFile());

        System.out.println(FileUtil.loopFiles("D:\\sss"));
        FileUtil.cleanEmpty(new File("D:\\sss"));
        System.out.println("----------------------------------------");
        System.out.println(FileUtil.loopFiles("D:\\sss"));

        String name = FileUtil.extName("C:\\Users\\Colorful\\Desktop\\Desktop.tar.gz");
        System.out.println(name);
    }


}
