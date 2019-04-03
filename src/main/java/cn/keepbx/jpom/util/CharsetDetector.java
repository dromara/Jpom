package cn.keepbx.jpom.util;

import org.mozilla.intl.chardet.nsDetector;
import org.mozilla.intl.chardet.nsPSMDetector;

import java.io.*;

/**
 * @author Administrator
 */
public class CharsetDetector {

    private boolean found = false;
    private String result;

    public String detectChineseCharset(File file) throws IOException {
        String[] val = detectChineseCharset(new FileInputStream(file));
        if (val == null || val.length <= 0) {
            return null;
        }
        return val[0];
    }

    public String[] detectChineseCharset(InputStream in) throws IOException {
        int lang = nsPSMDetector.CHINESE;
        String[] prob;
        // Initalize the nsDetector() ;
        nsDetector det = new nsDetector(lang);
        // Set an observer...
        // The Notify() will be called when a matching charset is found.
        det.Init(charset -> {
            found = true;
            result = charset;
        });
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
}