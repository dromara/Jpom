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
