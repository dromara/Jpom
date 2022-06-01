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
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.ZipUtil;
import org.junit.Test;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * Created by jiangzeyin on 2019/4/22.
 */
public class TestFile {
    public static void main(String[] args) throws IOException {
        File file = FileUtil.file("data123");
        System.out.println(FileUtil.isDirectory(file));

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

    @Test
    public void testReadFile() throws IOException {

        URL resource = ResourceUtil.getResource(".");
        String fileStr = resource.getFile();
        File file = FileUtil.file(fileStr);
        file = FileUtil.getParent(file, 2);
        file = FileUtil.file(file, "log", "info.log");
        BufferedReader reader = FileUtil.getReader(file, CharsetUtil.CHARSET_UTF_8);
        LineNumberReader lineNumberReader = new LineNumberReader(reader);
        lineNumberReader.setLineNumber(20);

        System.out.println(lineNumberReader.getLineNumber() + "  " + lineNumberReader.readLine());
        System.out.println(lineNumberReader.getLineNumber() + "  " + lineNumberReader.readLine());

        try (Stream<String> lines = Files.lines(Paths.get(file.getAbsolutePath()))) {
            String line32 = lines.skip(31).findFirst().get();
            System.out.println(line32);
            System.out.println(lines.skip(32).findFirst().get());
        }
//        FileUtil.tail();
    }
}
