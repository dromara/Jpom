/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.comparator.CompareUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;

import java.util.ArrayList;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author bwcx_jzy
 * @since 2023/6/14
 */
public class TestIntegral {

    public static void main(String[] args) {
        List<String> list = FileUtil.readLines("D:\\System-Data\\Documents\\WeChat Files\\A22838106\\FileStorage\\File\\2023-06\\积分签到.ini", CharsetUtil.CHARSET_GBK);
        List<List<String>> collect = list.stream()
            .filter(s -> StrUtil.contains(s, "目前积分"))
            .map(s -> StrUtil.splitTrim(s, "="))
            .collect(Collectors.toList());
        //
        Map<String, IntSummaryStatistics> statisticsMap = CollStreamUtil.groupBy(collect, strings -> strings.get(0), Collectors.summarizingInt(value -> Convert.toInt(value.get(1))));
        //
        List<Map.Entry<String, IntSummaryStatistics>> sortedList = new ArrayList<>(statisticsMap.entrySet());
        sortedList.sort(Map.Entry.comparingByValue((o1, o2) -> CompareUtil.compare(o2.getSum(), o1.getSum())));
        //
        List<Map.Entry<String, IntSummaryStatistics>> subbed = CollUtil.sub(sortedList, 0, 10);
        for (Map.Entry<String, IntSummaryStatistics> statisticsEntry : subbed) {
            System.out.println(statisticsEntry.getKey() + "  " + statisticsEntry.getValue().getSum());
        }
    }
}
