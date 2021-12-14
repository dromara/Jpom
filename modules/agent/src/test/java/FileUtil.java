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
//import cn.hutool.core.util.StrUtil;
//import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
//import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
//import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
//import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
//import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
//import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
//import org.apache.commons.compress.utils.IOUtils;
//
//import java.io.*;
//import java.util.ArrayList;
//import java.util.List;
//
//public class FileUtil {
//
//    public static int BUFFER_SIZE = 2048;
//
//    private static List<String> unTar(InputStream inputStream, String destDir) throws Exception {
//        List<String> fileNames = new ArrayList<String>();
//        TarArchiveInputStream tarIn = new TarArchiveInputStream(inputStream, BUFFER_SIZE);
//        TarArchiveEntry entry = null;
//        try {
//            while ((entry = tarIn.getNextTarEntry()) != null) {
//                fileNames.add(entry.getName());
//                if (entry.isDirectory()) {
//                    //是目录
//                    createDirectory(destDir, entry.getName());
//                    //创建空目录
//                } else {
//                    System.out.println(entry.getName());
//                    //是文件
//                    File tmpFile = new File(destDir + File.separator + entry.getName());
//                    //创建输出目录
//                    createDirectory(tmpFile.getParent() + File.separator, null);
//                    OutputStream out = null;
//                    try {
//                        out = new FileOutputStream(tmpFile);
//                        int length = 0;
//                        byte[] b = new byte[2048];
//                        while ((length = tarIn.read(b)) != -1) {
//                            out.write(b, 0, length);
//                        }
//                    } finally {
//                        IOUtils.closeQuietly(out);
//                    }
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            throw e;
//        } finally {
//            IOUtils.closeQuietly(tarIn);
//        }
//
//        return fileNames;
//    }
//
//    public static List<String> unTar(String tarFile, String destDir) throws Exception {
//        File file = new File(tarFile);
//        return unTar(file, destDir);
//    }
//
//    public static List<String> unTar(File tarFile, String destDir) throws Exception {
//        if (StrUtil.isBlank(destDir)) {
//            destDir = tarFile.getParent();
//        }
//        destDir = destDir.endsWith(File.separator) ? destDir : destDir + File.separator;
//        return unTar(new FileInputStream(tarFile), destDir);
//    }
//
//    public static List<String> unTarBZip2(File tarFile, String destDir) throws Exception {
//        if (StrUtil.isBlank(destDir)) {
//            destDir = tarFile.getParent();
//        }
//        destDir = destDir.endsWith(File.separator) ? destDir : destDir + File.separator;
//        return unTar(new BZip2CompressorInputStream(new FileInputStream(tarFile)), destDir);
//    }
//
//    public static List<String> unTarBZip2(String file, String destDir) throws Exception {
//        File tarFile = new File(file);
//        return unTarBZip2(tarFile, destDir);
//    }
//
//    public static List<String> unBZip2(String bzip2File, String destDir) throws IOException {
//        File file = new File(bzip2File);
//        return unBZip2(file, destDir);
//    }
//
//    public static List<String> unBZip2(File srcFile, String destDir) throws IOException {
//        if (StrUtil.isBlank(destDir)) {
//            destDir = srcFile.getParent();
//        }
//        destDir = destDir.endsWith(File.separator) ? destDir : destDir + File.separator;
//        List<String> fileNames = new ArrayList<>();
//        InputStream is = null;
//        OutputStream os = null;
//        try {
////            File destFile = new File(destDir, srcFile.getName());
//            String name = srcFile.getName();
//            name = name.replace(".bz2", "");
//            File destFile = new File(destDir, name);
//            fileNames.add(srcFile.getName());
//            is = new BZip2CompressorInputStream(new BufferedInputStream(new FileInputStream(srcFile), BUFFER_SIZE));
//            os = new BufferedOutputStream(new FileOutputStream(destFile), BUFFER_SIZE);
//            IOUtils.copy(is, os);
//        } finally {
//            IOUtils.closeQuietly(os);
//            IOUtils.closeQuietly(is);
//        }
//        return fileNames;
//    }
//
//    public static List<String> unGZ(String gzFile, String destDir) throws IOException {
//        File file = new File(gzFile);
//        return unGZ(file, destDir);
//    }
//
//    public static List<String> unGZ(File srcFile, String destDir) throws IOException {
//        if (StrUtil.isBlank(destDir)) {
//            destDir = srcFile.getParent();
//        }
//        destDir = destDir.endsWith(File.separator) ? destDir : destDir + File.separator;
//        List<String> fileNames = new ArrayList<String>();
//        InputStream is = null;
//        OutputStream os = null;
//        try {
//            String name = srcFile.getName();
//            name = name.replace(".gz", "");
//            File destFile = new File(destDir, name);
//            fileNames.add(srcFile.getName());
//            is = new GzipCompressorInputStream(new BufferedInputStream(new FileInputStream(srcFile), BUFFER_SIZE));
//            os = new BufferedOutputStream(new FileOutputStream(destFile), BUFFER_SIZE);
//            IOUtils.copy(is, os);
//        } finally {
//            IOUtils.closeQuietly(os);
//            IOUtils.closeQuietly(is);
//        }
//        return fileNames;
//    }
//
//    public static List<String> unTarGZ(File tarFile, String destDir) throws Exception {
//        if (StrUtil.isBlank(destDir)) {
//            destDir = tarFile.getParent();
//        }
//        destDir = destDir.endsWith(File.separator) ? destDir : destDir + File.separator;
//        return unTar(new GzipCompressorInputStream(new FileInputStream(tarFile)), destDir);
//    }
//
//    public static List<String> unTarGZ(String file, String destDir) throws Exception {
//        File tarFile = new File(file);
//        return unTarGZ(tarFile, destDir);
//    }
//
//    public static void createDirectory(String outputDir, String subDir) {
//        File file = new File(outputDir);
//        if (!(subDir == null || "".equals(subDir.trim()))) {
//            //子目录不为空
//            file = new File(outputDir + File.separator + subDir);
//        }
//        if (!file.exists()) {
//            file.mkdirs();
//        }
//    }
//
//    public static List<String> unZip(File zipfile, String destDir) throws Exception {
//        if (StrUtil.isBlank(destDir)) {
//            destDir = zipfile.getParent();
//        }
//        destDir = destDir.endsWith(File.separator) ? destDir : destDir + File.separator;
//        ZipArchiveInputStream is = null;
//        List<String> fileNames = new ArrayList<String>();
//
//        try {
//            is = new ZipArchiveInputStream(new BufferedInputStream(new FileInputStream(zipfile), BUFFER_SIZE));
//            ZipArchiveEntry entry = null;
//            while ((entry = is.getNextZipEntry()) != null) {
//                fileNames.add(entry.getName());
//                if (entry.isDirectory()) {
//                    File directory = new File(destDir, entry.getName());
//                    directory.mkdirs();
//                } else {
//                    OutputStream os = null;
//                    try {
//                        os = new BufferedOutputStream(new FileOutputStream(new File(destDir, entry.getName())), BUFFER_SIZE);
//                        IOUtils.copy(is, os);
//                    } finally {
//                        IOUtils.closeQuietly(os);
//                    }
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            throw e;
//        } finally {
//            IOUtils.closeQuietly(is);
//        }
//
//        return fileNames;
//    }
//
//    public static List<String> unZip(String zipfile, String destDir) throws Exception {
//        File zipFile = new File(zipfile);
//        return unZip(zipFile, destDir);
//    }
//
//    public static List<String> unCompress(String compressFile, String destDir) throws Exception {
//        String upperName = compressFile.toUpperCase();
//        List<String> ret = null;
//        if (upperName.endsWith(".ZIP")) {
//            ret = unZip(compressFile, destDir);
//        } else if (upperName.endsWith(".TAR")) {
//            ret = unTar(compressFile, destDir);
//        } else if (upperName.endsWith(".TAR.BZ2")) {
//            ret = unTarBZip2(compressFile, destDir);
//        } else if (upperName.endsWith(".BZ2")) {
//            ret = unBZip2(compressFile, destDir);
//        } else if (upperName.endsWith(".TAR.GZ")) {
//            ret = unTarGZ(compressFile, destDir);
//        } else if (upperName.endsWith(".GZ")) {
//            ret = unGZ(compressFile, destDir);
//        }
//        return ret;
//    }
//
//    public static void main(String[] args) throws Exception {
//        System.out.println(unGZ("D:\\systemDocument\\桌面\\zip\\aaa.txt.gz", "D:\\systemDocument\\桌面\\zip\\gz"));
//        System.out.println(unBZip2("D:\\systemDocument\\桌面\\zip\\aaa.txt.bz2", "D:\\systemDocument\\桌面\\zip\\bz"));
//
//
////        System.out.println(unZip("D:\\systemDocument\\桌面\\zip\\test.zip", "D:\\systemDocument\\桌面\\zip\\zip"));
////        System.out.println(unTar("D:\\systemDocument\\桌面\\zip\\aaa.tar", "D:\\systemDocument\\桌面\\zip\\tar"));
////        System.out.println(unTarBZip2("D:\\systemDocument\\桌面\\zip\\aaa.tar.bz2", "D:\\systemDocument\\桌面\\zip\\tarbz"));
////        System.out.println(unTarGZ("D:\\systemDocument\\桌面\\zip\\apache-zookeeper-3.5.5.tar.gz", "D:\\systemDocument\\桌面\\zip\\targz"));
//
//
////        unCompress("D:\\systemDocument\\桌面\\zip\\test.zip", "D:\\systemDocument\\桌面\\zip\\a");
////        System.out.println(unZip("D:\\systemDocument\\桌面\\zip\\aaa.tar", "D:\\systemDocument\\桌面\\zip\\a"));
////        System.out.println(unTar("F:\\fileupload\\中文test.tar", "F:\\fileupload\\"));
//
//        //System.out.println(unBZip2("F:\\fileupload\\中文test.xml.bz2", "F:\\fileupload\\"));
//        //System.out.println(unTarBZip2("F:\\fileupload\\中文test.tar.bz2", "F:\\fileupload\\"));
//
//        //System.out.println(unGZ("F:\\fileupload\\test.xml.gz", "F:\\fileupload\\"));
//        //System.out.println(unTarGZ("F:\\fileupload\\all.tar.gz", "F:\\fileupload\\"));
//    }
//}