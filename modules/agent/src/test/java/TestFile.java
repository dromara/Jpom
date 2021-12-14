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
