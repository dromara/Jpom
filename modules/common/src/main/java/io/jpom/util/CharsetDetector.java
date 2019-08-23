package io.jpom.util;

import org.mozilla.intl.chardet.nsDetector;
import org.mozilla.intl.chardet.nsICharsetDetectionObserver;
import org.mozilla.intl.chardet.nsPSMDetector;

import java.io.*;

/**
 * 文件编码识别器
 *
 * @author Administrator
 */
public class CharsetDetector implements nsICharsetDetectionObserver {

    private boolean found = false;
    private String result;

    public String detectChineseCharset(File file) throws IOException {
        if (!file.exists()) {
            throw new FileNotFoundException(file.getAbsolutePath());
        }
        String[] val = detectChineseCharset(new FileInputStream(file));
        if (val == null || val.length <= 0) {
            return null;
        }
        return val[0];
    }

    private String[] detectChineseCharset(InputStream in) throws IOException {
        // Initalize the nsDetector() ;
        nsDetector det = new nsDetector(nsPSMDetector.CHINESE);
        // Set an observer...
        // The Notify() will be called when a matching charset is found.
        det.Init(this);
        BufferedInputStream imp = new BufferedInputStream(in);
        byte[] buf = new byte[1024];
        int len;
        boolean isAscii = true;
        while ((len = imp.read(buf, 0, buf.length)) != -1) {
            // Check if the stream is only ascii.
            if (isAscii) {
                isAscii = det.isAscii(buf, len);
            }
            // DoIt if non-ascii and not done yet.
            if (!isAscii) {
                if (det.DoIt(buf, len, false)) {
                    break;
                }
            }
        }
        imp.close();
        in.close();
        det.DataEnd();
        String[] prob;
        if (isAscii) {
            found = true;
            prob = new String[]{"ASCII"};
        } else if (found) {
            prob = new String[]{result};
        } else {
            prob = det.getProbableCharsets();
        }
        return prob;
    }

    @Override
    public void Notify(String charset) {
        found = true;
        result = charset;
    }
}