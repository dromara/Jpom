import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ZipUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by jiangzeyin on 2019/4/22.
 */
public class TestFile {
    public static void main(String[] args) throws IOException {
        InputStream inputStream = new FileInputStream("D:\\SystemDocument\\Desktop\\Desktop.zip");

        String code = IoUtil.readHex28Upper(inputStream);
        System.out.println(code);

        System.out.println(FileUtil.getMimeType("D:\\SystemDocument\\Desktop\\Desktop.zip"));


        System.out.println(FileUtil.getMimeType("D:\\SystemDocument\\Desktop\\Desktop.tar.gz"));

        System.out.println(FileUtil.getMimeType("D:\\SystemDocument\\Desktop\\Desktop.7z"));

        ZipUtil.unzip(new File("D:\\SystemDocument\\Desktop\\Desktop.tar.gz"), new File("D:\\SystemDocument\\Desktop\\Desktop.7z\""));

        ZipUtil.unzip(new File("D:\\SystemDocument\\Desktop\\Desktop.7z"), new File("D:\\SystemDocument\\Desktop\\Desktop.7z\""));

        System.out.println(FileUtil.extName("test.zip"));
    }
}
