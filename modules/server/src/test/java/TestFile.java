import cn.hutool.core.io.FileUtil;
import org.junit.Test;

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
        File file = FileUtil.file("D:\\jpom\\server\\data\\build\\39a61a05c63b4f56baf0d90bad498ac2\\history\\#7");
        System.out.println(FileUtil.mainName(file));
    }


    @Test
    public void testFile() {
        File file = FileUtil.file("D:\\Idea\\hutool\\.git");
        System.out.println(file.isHidden());
    }

}
