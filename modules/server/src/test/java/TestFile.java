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
