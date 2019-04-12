import cn.keepbx.jpom.util.CharsetDetector;

import java.io.File;
import java.io.IOException;

/**
 * Created by jiangzeyin on 2019/3/15.
 */
public class TestFile {
    public static void main(String[] args) throws IOException {
        File file = new File("C:/WINDOWS/system32/s/s");
        System.out.println(file.toPath().startsWith(new File("C:/Windows/System32/s/S").toPath()));
//        System.out.println(file());


        File file1 = new File("D:/jpom-test/test2.log");
        System.out.println(new CharsetDetector().detectChineseCharset(file1));
    }


}
