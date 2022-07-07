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
import cn.hutool.core.lang.PatternPool;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.DigestAlgorithm;
import cn.hutool.crypto.digest.Digester;
import org.junit.Test;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.SortedMap;
import java.util.regex.Pattern;

/**
 * Created by jiangzeyin on 2019/3/1.
 */
public class TestString {

    @Test
    public void testCharset() {
        Charset utf8 = CharsetUtil.charset("utf8");
        Charset utf82 = CharsetUtil.charset("utf-8");
        System.out.println(utf82 == utf8);
    }

    @Test
    public void test2() {
        System.out.println(StrUtil.format("#{{}}", 1)
        );

        String replace = "#{A}";
        replace = StrUtil.replace(replace, "#{AAAAAAA}", "1");
        System.out.println(replace);
    }

    @Test
    public void test() {
        String s = ReUtil.get("<title>.*?</title>", "<a>aa</a><title>sss</title>", 0);
        System.out.println(s);
    }

    public static void main(String[] args) {
//        System.out.println(CheckPassword.checkPassword("123aA!"));
//        DateTime dateTime = DateUtil.parseUTC("2019-04-04T10:11:21Z");
//        System.out.println(dateTime);
//        dateTime.setTimeZone(TimeZone.getDefault());
//        System.out.println(dateTime);
        Pattern pattern = Pattern.compile("(https://|http://)?([\\w-]+\\.)+[\\w-]+(:\\d+|/)+([\\w- ./?%&=]*)?");
        String url = "http://192.168.1.111:2122/node/index.html?nodeId=dyc";
        System.out.println(ReUtil.isMatch(pattern, url));
        System.out.println(ReUtil.isMatch(PatternPool.URL_HTTP, url));


//        System.out.println(FileUtil.file("/a", null, "", "ss"));

        System.out.println(Math.pow(1024, 2));

        System.out.println(Integer.MAX_VALUE);

        while (true) {
            SortedMap<String, Charset> x = Charset.availableCharsets();
            Collection<Charset> values = x.values();
            boolean find = false;
            for (Charset charset : values) {
                String name = charset.name();
                if ("utf-8".equalsIgnoreCase(name)) {
                    find = true;
                    break;
                }
            }
            if (!find) {
                System.out.println("没有找utf-8");
            }
        }
//        System.out.println(x);
    }

    @Test
    public void test1() {
        System.out.println(SecureUtil.sha256("1"));

        System.out.println(SecureUtil.sha256("admin"));


        int randomInt = 2;
        RandomUtil.randomInt(1, 100);
        System.out.println(randomInt);
        String nowStr = "admin";
        nowStr = new Digester(DigestAlgorithm.SHA256).setDigestCount(2).digestHex(nowStr);
        System.out.println(nowStr);
    }

    @Test
    public void testLen() {
        System.out.println(StrUtil.EMPTY.length() + "  " + StrUtil.EMPTY.isEmpty());
    }
}
