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
package git;

import cn.hutool.core.comparator.VersionComparator;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author bwcx_jzy
 * @since 2023/9/15
 */
public class TestSort {

    @Test
    public void test() {
        File file = FileUtil.file("D:\\System-Data\\Documents\\WeChat Files\\A22838106\\FileStorage\\File\\2023-09\\list");
        List<String> list = FileUtil.readLines(file, CharsetUtil.CHARSET_UTF_8);
        //
        list = list.stream()
            .map(StrUtil::trim).map(name -> {
                //String name = ref.getName();
                if (name.startsWith("remotes/origin/")) {
                    return name.substring("remotes/origin/".length());
                }
                return null;
            })
            .filter(Objects::nonNull)
            .sorted((o1, o2) -> {
                int compare = VersionComparator.INSTANCE.compare(o2, o1);
                System.out.println(compare);
                System.out.println(o1 + "  " + o2);
                return compare;
            })
            .collect(Collectors.toList());
        //list.sort((o1, o2) -> VersionComparator.INSTANCE.compare(o2, o1));
        System.out.println(list);
    }

    @Test
    public void test2() {
        List<String> list = new ArrayList<>();
        list.add("lester-hotfix-202308002-1");
        list.add("lester-invoice-release");
        list.sort(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                int compare = VersionComparator.INSTANCE.compare(o2, o1);
                return compare;
            }
        });

    }
}
