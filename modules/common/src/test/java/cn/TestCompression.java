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

    @Test
    public void test2(){
       File file = new File("/Users/user/压缩文件.tbz");
        File dir = FileUtil.file("/Users/user/unc");
        CompressionFileUtil.unCompress(file, dir);
    }
}
