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
import io.jpom.util.HttpUtils;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author bwcx_jzy
 * @since 2019/11/18
 */
public class TestHttp {

    @Test
    public void test() throws UnsupportedEncodingException {
        Map<String, String> header = new HashMap<>();
        Map<String, String> form = new HashMap<>();
        String s = "http://127.0.0.1:9001/iptv_jpom//node/manage/saveProject";

        header.put("JPOM-USER-TOKEN", "5610b7db99f7216e4ed3543f2a56eb95");
//        //
        form.put("nodeId", "localhost");
//        //
        form.put("name", ":");
        form.put("group", "ss");
        form.put("id", "ss");
        form.put("runMode", "File");
        form.put("whitelistDirectory", "/test/");
        form.put("lib", "dfgdsfg");
        //
        form.put("mainClass", "");
        form.put("jvm", "");
        form.put("args", "");
        form.put("token", "");
        String post = HttpUtils.post(s, form, header, (int) TimeUnit.MINUTES.toMillis(1), (int) TimeUnit.MINUTES.toMillis(1), "utf-8");
        System.out.println(post);
    }
}
