/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.io.unit.DataSizeUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.ZipUtil;
import org.junit.Test;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * Created by bwcx_jzy on 2019/4/22.
 */
public class TestFile {
    public static void main(String[] args) throws IOException {
        File file = FileUtil.file("data123");
        System.out.println(FileUtil.isDirectory(file));

        InputStream inputStream = new FileInputStream("D:\\SystemDocument\\Desktop\\Desktop.zip");

        String code = IoUtil.readHex64Upper(inputStream);
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

    @Test
    public void test() {
        System.out.println(DataSizeUtil.format(1000));
    }
}
