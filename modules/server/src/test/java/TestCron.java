/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
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
        CronUtil.schedule(CRON_ID, "0/5 * * * * ?", () -> System.out.println("123"));
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
