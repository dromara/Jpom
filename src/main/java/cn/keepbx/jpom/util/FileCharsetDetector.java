package cn.keepbx.jpom.util;

import org.mozilla.intl.chardet.nsDetector;
import org.mozilla.intl.chardet.nsICharsetDetectionObserver;

import java.io.*;

/**
 * 借助JCharDet获取文件字符集
 *
 * @author robin
 */
public class FileCharsetDetector {

    /**
     * 字符集名称
     */
    private static String encoding;

    /**
     * 字符集是否已检测到
     */
    private static boolean found;

    private static nsDetector detector;

    private static nsICharsetDetectionObserver observer;

    /**
     * 适应语言枚举
     *
     * @author robin
     */
    public enum Language {
        Japanese(1),
        Chinese(2),
        SimplifiedChinese(3),
        TraditionalChinese(4),
        Korean(5),
        DontKnow(6);

        private int hint;

        Language(int hint) {
            this.hint = hint;
        }

        public int getHint() {
            return this.hint;
        }
    }

    /**
     * 传入一个文件(File)对象，检查文件编码
     *
     * @param file File对象实例
     * @return 文件编码，若无，则返回null
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static String checkEncoding(File file) throws FileNotFoundException,
            IOException {
        String test = checkEncoding(file, getNsdetector());
        System.out.println(test);
        return test;
    }

    /**
     * 获取文件的编码
     *
     * @param file     File对象实例
     * @param language 语言
     * @return 文件编码
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static String checkEncoding(File file, Language lang)
            throws FileNotFoundException, IOException {
        return checkEncoding(file, new nsDetector(lang.getHint()));
    }

    /**
     * 获取文件的编码
     *
     * @param path 文件路径
     * @return 文件编码，eg：UTF-8,GBK,GB2312形式，若无，则返回null
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static String checkEncoding(String path) throws FileNotFoundException,
            IOException {
        return checkEncoding(new File(path));
    }

    /**
     * 获取文件的编码
     *
     * @param path     文件路径
     * @param language 语言
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static String checkEncoding(String path, Language lang)
            throws FileNotFoundException, IOException {
        return checkEncoding(new File(path), lang);
    }

    /**
     * 获取文件的编码
     *
     * @param file
     * @param det
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    private static String checkEncoding(File file, nsDetector detector)
            throws FileNotFoundException, IOException {

        detector.Init(getCharsetDetectionObserver());

        if (isAscii(file, detector)) {
            encoding = "ASCII";
            found = true;
        }

        if (!found) {
            String prob[] = detector.getProbableCharsets();
            if (prob.length > 0) {
                encoding = prob[0];
            } else {
                return null;
            }
        }

        return encoding;
    }

    /**
     * 检查文件编码类型是否是ASCII型
     *
     * @param file     要检查编码的文件
     * @param detector
     * @return
     * @throws IOException
     */
    private static boolean isAscii(File file, nsDetector detector) throws IOException {
        BufferedInputStream input = null;
        try {
            input = new BufferedInputStream(new FileInputStream(file));

            byte[] buffer = new byte[1024];
            int hasRead;
            boolean done = false;
            boolean isAscii = true;

            while ((hasRead = input.read(buffer)) != -1) {
                if (isAscii)
                    isAscii = detector.isAscii(buffer, hasRead);
                if (!isAscii && !done)
                    done = detector.DoIt(buffer, hasRead, false);
            }

            return isAscii;
        } finally {
            detector.DataEnd();
            if (null != input) input.close();
        }
    }

    /**
     * nsDetector单例创建
     *
     * @return
     */
    private static nsDetector getNsdetector() {
        if (null == detector) {
            detector = new nsDetector();
        }
        return detector;
    }

    /**
     * nsICharsetDetectionObserver 单例创建
     *
     * @return
     */
    private static nsICharsetDetectionObserver getCharsetDetectionObserver() {
        if (null == observer) {
            observer = charset -> {
                found = true;
                encoding = charset;
            };
        }
        return observer;
    }
}