package cn;

import cn.hutool.core.io.FileUtil;
import io.jpom.util.CompressionFileUtil;
import org.junit.Test;

import java.io.File;

/**
 * @author bwcx_jzy
 * @since 2023/2/13
 */
public class TestCompression {

    @Test
    public void test() {
        File file = new File("D:\\System-Data\\Documents\\WeChat Files\\A22838106\\FileStorage\\File\\2023-02\\$R7JOOR8.tar.gz");
        File dir = FileUtil.file("D:\\temp\\unc");
        CompressionFileUtil.unCompress(file, dir);
    }
}
