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
import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.PatternPool;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.net.url.UrlQuery;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.DigestAlgorithm;
import cn.hutool.crypto.digest.Digester;
import org.junit.Test;
import org.springframework.boot.convert.DurationStyle;

import java.nio.charset.Charset;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Created by bwcx_jzy on 2019/3/1.
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

    @Test
    public void testStream() {
        Stream<Integer> integerStream = Stream.of(1, 2, 3);
        System.out.println(integerStream.count());
        System.out.println(integerStream.count());
    }

    @Test
    public void testBase64() {
        String encode = Base64.decodeStr("YWJjZA");
        System.out.println(encode);
    }

    @Test
    public void testMapStr() {
        UrlQuery urlQuery = UrlQuery.of("xx=xx&xxx=xxx", CharsetUtil.CHARSET_UTF_8);
//        urlQuery.getQueryMap()
        HashMap<String, String> map = MapUtil.of("sss", "xxxxx");
        map.put("ss", "23");
        System.out.println(MapUtil.join(map, ";", "="));
    }

    @Test
    public void testTime() {
        String time = "5h";
        DurationStyle durationStyle = DurationStyle.detect(time);
        Duration duration = durationStyle.parse(time);
        System.out.println(duration.toHours());

        System.out.println(Duration.ofHours(5).toHours());
//        Duration convert1 = DefaultConversionService.getSharedInstance().convert("18000", Duration.class);
//        System.out.println(convert1);
//        Duration convert = Convert.convert(Duration.class, "18000");
//        System.out.println(convert);

//        System.out.println(Duration.parse("18000"));
//        DateUnit dateUnit = DateUnit.valueOf("18000");
    }

    @Test
    public void testVersion() {
        System.out.println(StrUtil.compareVersion("2.10.10", "2.10.9"));
        System.out.println(StrUtil.compareVersion("2.10.37", "2.10.38"));
        System.out.println(StrUtil.compareVersion("2.10.37", "2.10.37.1"));
        System.out.println(StrUtil.compareVersion("2.10.38", "2.10.37.9"));
    }

    @Test
    public void testArrayList() {
        ArrayList<Integer> integers = CollUtil.newArrayList(1, 2);
        System.out.println(CollUtil.sub(integers, 0 + 1, integers.size()));
    }

    @Test
    public void testLong() {
        System.out.println(4045003435L - 5476659199L);
    }

    @Test
    public void testMap() {
        Map<String, String> map = new HashMap<>();
        map.put("a", "1");

        Set<String> set = new HashSet<>(map.keySet());
        map.put("b", "2");
        System.out.println(set);
    }
}
