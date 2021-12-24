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
package io.jpom.plugin;

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
