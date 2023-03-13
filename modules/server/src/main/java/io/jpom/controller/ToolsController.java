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
package io.jpom.controller;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.cron.pattern.CronPatternUtil;
import io.jpom.common.JsonMessage;
import io.jpom.common.validator.ValidatorItem;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author bwcx_jzy
 * @since 2023/3/10
 */
@RestController
@RequestMapping(value = "/tools")
public class ToolsController {

    @GetMapping(value = "cron", produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonMessage<List<Long>> cron(@ValidatorItem String cron, @ValidatorItem int count, String date, boolean isMatchSecond) {
        Date startDate = null;
        Date endDate = null;
        if (StrUtil.isNotEmpty(date)) {
            List<String> split = StrUtil.splitTrim(date, "~");
            try {
                startDate = DateUtil.parse(split.get(0));
                startDate = DateUtil.beginOfDay(startDate);
                endDate = DateUtil.parse(split.get(1));
                endDate = DateUtil.endOfDay(endDate);
            } catch (Exception e) {
                return new JsonMessage<>(405, "日期格式错误:" + e.getMessage());
            }
        }
        try {
            List<Date> dateList;
            if (startDate != null) {
                dateList = CronPatternUtil.matchedDates(cron, startDate, endDate, count, isMatchSecond);
            } else {
                dateList = CronPatternUtil.matchedDates(cron, DateTime.now(), count, isMatchSecond);
            }
            return JsonMessage.success("", dateList.stream().map(Date::getTime).collect(Collectors.toList()));
        } catch (Exception e) {
            return new JsonMessage<>(405, "cron 表达式不正确," + e.getMessage());
        }
    }
}
