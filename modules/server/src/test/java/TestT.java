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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.cron.pattern.CronPatternUtil;
import io.jpom.util.AntPathUtil;
import io.jpom.util.FileUtils;
import org.junit.Test;

import java.io.File;
import java.time.Duration;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * @author bwcx_jzy
 * @since 2019/9/4
 */
public class TestT {

    @Test
    public void tset() {
        List<Date> dates = CronPatternUtil
            .matchedDates("0 0 0 1/3 * ?", DateTime.now(),
                DateUtil.offset(DateTime.now(), DateField.YEAR, 1), 10, true);
        for (Date date : dates) {
            System.out.println(DateUtil.format(date, DatePattern.NORM_DATETIME_MS_PATTERN));
        }
    }

    public static void main(String[] args) {
        Duration parse = Duration.parse("1H");
        System.out.println(parse.getSeconds());
    }

    @Test
    public void testList() {
        List<Integer> list = CollUtil.newArrayList(1, 2, 3, 4, 5, 6);
        list.sort(Comparator.reverseOrder());
        int size = CollUtil.size(list);
        list = CollUtil.sub(list, 2, size);
        System.out.println(list);
    }


    @Test
    public void testChar() {
        String str = "abc";
        char[] chars = str.toCharArray();
        Character item = ArrayUtil.get(chars, 10);
        System.out.println(item);

        item = ArrayUtil.get(chars, 1);
        System.out.println(item);

        String pattern = "/sss**";
        String path = "/sss/xxx";

        System.out.println(AntPathUtil.ANT_PATH_MATCHER.match(pattern, path));
    }

    @Test
    public void testMatchPath() {
        File file = FileUtil.file("C:\\Users\\bwcx_\\jpom\\server\\data\\build\\1b5c98e58e334f0e9b70b09c455d9dfd\\source");
//        String match = "/springboot-test*/s?c/**/*.java";
        String match = "/**/*.xml";
//        String match = "/springboot-test-jar/**/*.*";
        List<String> matcher = AntPathUtil.antPathMatcher(file, match);
//        String subMatch = "/springboot-test-*/src/";
        String subMatch = "/springboot-test-**/src/";
        for (String s : matcher) {
            List<String> list = StrUtil.splitTrim(s, StrUtil.SLASH);
            if (!AntPathUtil.ANT_PATH_MATCHER.matchStart(subMatch + "**", s)) {
                continue;
            }
            int notMathIndex = ArrayUtil.INDEX_NOT_FOUND;
            int size = list.size();
            for (int i = size - 1; i >= 0; i--) {
                String suffix = i == size - 1 ? StrUtil.EMPTY : StrUtil.SLASH;
                String itemS = StrUtil.SLASH + CollUtil.join(CollUtil.sub(list, 0, i + 1), StrUtil.SLASH) + suffix;
                //System.out.println(subMatch + " o " + itemS);
//                System.out.println(AntPathUtil.ANT_PATH_MATCHER.extractPathWithinPattern(subMatch, itemS));
                if (AntPathUtil.ANT_PATH_MATCHER.match(subMatch, itemS)) {
                    System.out.println(subMatch + "   " + itemS + "  ");
                    notMathIndex = i + 1;
                    break;
                }
            }
//            for (int i = 0; i < list.size(); i++) {
//                String suffix = i == list.size() - 1 ? StrUtil.EMPTY : StrUtil.SLASH;
//                String itemS = StrUtil.SLASH + CollUtil.join(CollUtil.sub(list, 0, i + 1), StrUtil.SLASH) + suffix;
//                // String itemS = StrUtil.SLASH + CollUtil.join(CollUtil.sub(list, 0, i + 1), StrUtil.SLASH);
//                if (i == 0) {
//                    if (!AntPathUtil.ANT_PATH_MATCHER.match(subMatch, itemS)) {
//
//                    }
//                }
//                System.out.println(subMatch + " o " + itemS);
////                System.out.println(AntPathUtil.ANT_PATH_MATCHER.extractPathWithinPattern(subMatch, itemS));
//                if (!AntPathUtil.ANT_PATH_MATCHER.match(subMatch, itemS)) {
//                    System.out.println(subMatch + "   " + itemS + "  ");
//                    notMathIndex = i;
//                    break;
//                }
//            }
            if (notMathIndex == ArrayUtil.INDEX_NOT_FOUND) {
                continue;
            }
            String itemEnd = CollUtil.join(CollUtil.sub(list, notMathIndex, size), StrUtil.SLASH);
            System.out.println(notMathIndex + " ok: " + s + "  " + itemEnd);
            //break;

        }
        System.out.println("123456");


        System.out.println(matcher);
    }

    @Test
    public void testFile() {
        FileUtils.checkSlip("/../../../xxx/xx//aaa/../");
        FileUtils.checkSlip("/../../../xxx/xx?/&&{#}:/aaa/../");
    }

    @Test
    public void testDownload() {
//        HttpUtil.download()
    }
}
