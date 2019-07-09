package cn.keepbx.jpom.util;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 压缩文件工具
 *
 * @author bwcx_jzy
 */
public class CompressionFileUtil {

    private static int BUFFER_SIZE = 2048;

    private static List<String> unTar(String tarFile, String destDir) throws Exception {
        File file = new File(tarFile);
        return unTar(file, destDir);
    }

    private static List<String> unTar(File tarFile, String destDir) throws Exception {
        if (StrUtil.isBlank(destDir)) {
            destDir = tarFile.getParent();
        }
        destDir = destDir.endsWith(File.separator) ? destDir : destDir + File.separator;
        return unTar(new FileInputStream(tarFile), destDir);
    }

    private static List<String> unTarBZip2(String file, String destDir) throws Exception {
        File tarFile = new File(file);
        return unTarBZip2(tarFile, destDir);
    }

    private static List<String> unTarBZip2(File tarFile, String destDir) throws Exception {
        if (StrUtil.isBlank(destDir)) {
            destDir = tarFile.getParent();
        }
        destDir = destDir.endsWith(File.separator) ? destDir : destDir + File.separator;
        return unTar(new BZip2CompressorInputStream(new FileInputStream(tarFile)), destDir);
    }

    private static List<String> unBZip2(String bzip2File, String destDir) throws IOException {
        File file = new File(bzip2File);
        return unBZip2(file, destDir);
    }

    private static List<String> unTarGZ(File tarFile, String destDir) throws Exception {
        if (StrUtil.isBlank(destDir)) {
            destDir = tarFile.getParent();
        }
        destDir = destDir.endsWith(File.separator) ? destDir : destDir + File.separator;
        return unTar(new GzipCompressorInputStream(new FileInputStream(tarFile)), destDir);
    }

    private static List<String> unTarGZ(String file, String destDir) throws Exception {
        File tarFile = new File(file);
        return unTarGZ(tarFile, destDir);
    }

    private static List<String> unZip(String zipfile, String destDir) throws Exception {
        File zipFile = new File(zipfile);
        return unZip(zipFile, destDir);
    }

    private static List<String> unGZ(String gzFile, String destDir) throws IOException {
        File file = new File(gzFile);
        return unGZ(file, destDir);
    }

    private static void createDirectory(String outputDir, String subDir) {
        File file = new File(outputDir);
        if (!(subDir == null || "".equals(subDir.trim()))) {
            //子目录不为空
            file = new File(outputDir + File.separator + subDir);
        }
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    private static List<String> unTar(InputStream inputStream, String destDir) throws Exception {
        List<String> fileNames = new ArrayList<>();
        TarArchiveInputStream tarIn = new TarArchiveInputStream(inputStream, BUFFER_SIZE);
        TarArchiveEntry entry;
        try {
            while ((entry = tarIn.getNextTarEntry()) != null) {
                fileNames.add(entry.getName());
                if (entry.isDirectory()) {
                    //是目录
                    createDirectory(destDir, entry.getName());
                    //创建空目录
                } else {
                    //是文件
                    File tmpFile = new File(destDir + File.separator + entry.getName());
                    //创建输出目录
                    createDirectory(tmpFile.getParent() + File.separator, null);
                    OutputStream out = null;
                    try {
                        out = new FileOutputStream(tmpFile);
                        int length;
                        byte[] b = new byte[2048];
                        while ((length = tarIn.read(b)) != -1) {
                            out.write(b, 0, length);
                        }
                    } finally {
                        IOUtils.closeQuietly(out);
                    }
                }
            }
        } finally {
            IOUtils.closeQuietly(tarIn);
        }
        return fileNames;
    }

    private static List<String> unBZip2(File srcFile, String destDir) throws IOException {
        if (StrUtil.isBlank(destDir)) {
            destDir = srcFile.getParent();
        }
        destDir = destDir.endsWith(File.separator) ? destDir : destDir + File.separator;
        List<String> fileNames = new ArrayList<>();
        InputStream is = null;
        OutputStream os = null;
        try {
            String name = srcFile.getName().replace(".bz2", "");
            File destFile = new File(destDir, name);
            fileNames.add(srcFile.getName());
            is = new BZip2CompressorInputStream(new BufferedInputStream(new FileInputStream(srcFile), BUFFER_SIZE));
            os = new BufferedOutputStream(new FileOutputStream(destFile), BUFFER_SIZE);
            IOUtils.copy(is, os);
        } finally {
            IOUtils.closeQuietly(os);
            IOUtils.closeQuietly(is);
        }
        return fileNames;
    }

    private static List<String> unGZ(File srcFile, String destDir) throws IOException {
        if (StrUtil.isBlank(destDir)) {
            destDir = srcFile.getParent();
        }
        List<String> fileNames = new ArrayList<>();
        destDir = destDir.endsWith(File.separator) ? destDir : destDir + File.separator;
        InputStream is = null;
        OutputStream os = null;
        try {
            String name = srcFile.getName().replace(".gz", "");
            File destFile = new File(destDir, name);
            fileNames.add(srcFile.getName());
            os = new BufferedOutputStream(new FileOutputStream(destFile), BUFFER_SIZE);
            is = new GzipCompressorInputStream(new BufferedInputStream(new FileInputStream(srcFile), BUFFER_SIZE));
            IOUtils.copy(is, os);
        } finally {
            IOUtils.closeQuietly(os);
            IOUtils.closeQuietly(is);
        }
        return fileNames;
    }

    private static List<String> unZip(File zipfile, String destDir) throws Exception {
        if (StrUtil.isBlank(destDir)) {
            destDir = zipfile.getParent();
        }
        destDir = destDir.endsWith(File.separator) ? destDir : destDir + File.separator;
        ZipArchiveInputStream is = null;
        List<String> fileNames = new ArrayList<>();

        try {
            is = new ZipArchiveInputStream(new BufferedInputStream(new FileInputStream(zipfile), BUFFER_SIZE), CharsetUtil.GBK);
            ZipArchiveEntry entry;
            while ((entry = is.getNextZipEntry()) != null) {
                fileNames.add(entry.getName());
                if (entry.isDirectory()) {
                    File directory = new File(destDir, entry.getName());
                    directory.mkdirs();
                } else {
                    OutputStream os = null;
                    try {
                        os = new BufferedOutputStream(new FileOutputStream(new File(destDir, entry.getName())), BUFFER_SIZE);
                        IOUtils.copy(is, os);
                    } finally {
                        IOUtils.closeQuietly(os);
                    }
                }
            }
        } finally {
            IOUtils.closeQuietly(is);
        }
        return fileNames;
    }

    public static List<String> unCompress(String compressFile, String destDir) throws Exception {
        String upperName = compressFile.toUpperCase();
        List<String> ret = null;
        if (upperName.endsWith(".ZIP")) {
            ret = unZip(compressFile, destDir);
        } else if (upperName.endsWith(".TAR")) {
            ret = unTar(compressFile, destDir);
        } else if (upperName.endsWith(".TAR.BZ2")) {
            ret = unTarBZip2(compressFile, destDir);
        } else if (upperName.endsWith(".BZ2")) {
            ret = unBZip2(compressFile, destDir);
        } else if (upperName.endsWith(".TAR.GZ")) {
            ret = unTarGZ(compressFile, destDir);
        } else if (upperName.endsWith(".GZ")) {
            ret = unGZ(compressFile, destDir);
        }
        return ret;
    }
}
