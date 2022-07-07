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
package cn;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.cron.pattern.CronPatternUtil;
import org.junit.Test;

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


}
