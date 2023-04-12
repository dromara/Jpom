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
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.cron.CronUtil;
import cn.hutool.cron.pattern.CronPattern;
import cn.hutool.cron.pattern.CronPatternUtil;
import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by bwcx_jzy on 2019/3/4.
 */
public class TestCron {
    public static void main(String[] args) {
        String CRON_ID = "test";
        CronUtil.remove(CRON_ID);
        CronUtil.setMatchSecond(true);
        CronUtil.schedule(CRON_ID, "0/5 * * * * ?", () -> {
            System.out.println("123");
        });
        CronUtil.restart();
//        System.out.println(JpomApplicationEvent.getPid());
    }

    @Test
    public void test() {
        String cron = "0 0 23 ? * 5 ";

        CronPattern cronPattern = CronPattern.of(cron);

//        Date date = CronPatternUtil.nextDateAfter(cronPattern, DateUtil.offsetDay(DateTime.now(), -1), false);

        List<Date> dateList = CronPatternUtil.matchedDates(cron, DateUtil.offsetDay(DateTime.now(), -1), 10, true);
        for (Date date1 : dateList) {
            System.out.println(DateUtil.format(date1, DatePattern.NORM_DATETIME_FORMAT));
        }

    }

    @Test
    public void test2() {
        Calendar calendar = DateTime.now().toCalendar();
        System.out.println(calendar.get(Calendar.DAY_OF_WEEK) - 1);
    }
}
