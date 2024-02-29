/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.controller;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.cron.pattern.CronPatternUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import org.dromara.jpom.common.validator.ValidatorItem;
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
    public IJsonMessage<List<Long>> cron(@ValidatorItem String cron, @ValidatorItem int count, String date, boolean isMatchSecond) {
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
