import java.io.File;
import java.io.IOException;

/**
 * Created by jiangzeyin on 2019/3/15.
 */
public class TestFile {
    public static void main(String[] args) throws IOException {
        File file = new File("C:/WINDOWS/system32/");
        System.out.println(file.getAbsolutePath());
        System.out.println(file.getCanonicalPath());
//        System.out.println(file());
    }
}
